package com.campus.exchange.service;

import com.campus.exchange.backup.ResourceBackupItem;
import com.campus.exchange.backup.ResourceCatalogBackup;
import com.campus.exchange.dao.ResourceDAO;
import com.campus.exchange.dao.UserDAO;
import com.campus.exchange.exception.AppException;
import com.campus.exchange.exception.ValidationException;
import com.campus.exchange.model.Resource;
import com.campus.exchange.model.enums.ResourceStatus;
import com.campus.exchange.util.AppConfig;
import com.campus.exchange.util.DbConnection;
import com.campus.exchange.util.DbErrors;

import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Administrator-only catalogue backup and additive restore using Java
 * serialization with a strict deserialization allowlist.
 */
public class BackupService {

    private static final Logger LOG = Logger.getLogger(BackupService.class.getName());
    private static final Pattern SAFE_NAME =
            Pattern.compile("^catalog-\\d{8}-\\d{6}-\\d{3,6}\\.ser$");
    private static final DateTimeFormatter FILE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final int MAX_ITEMS = 10_000;

    private static final ObjectInputFilter FILTER = info -> {
        if (info.depth() > 20 || info.references() > 100_000
                || info.arrayLength() > MAX_ITEMS) {
            return ObjectInputFilter.Status.REJECTED;
        }
        Class<?> serialClass = info.serialClass();
        if (serialClass == null) {
            return ObjectInputFilter.Status.UNDECIDED;
        }
        if (serialClass.isPrimitive()) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        if (serialClass.isArray()) {
            Class<?> component = serialClass.getComponentType();
            while (component.isArray()) {
                component = component.getComponentType();
            }
            return component.isPrimitive() || component == Object.class || allowed(component)
                    ? ObjectInputFilter.Status.ALLOWED
                    : ObjectInputFilter.Status.REJECTED;
        }
        return allowed(serialClass)
                ? ObjectInputFilter.Status.ALLOWED
                : ObjectInputFilter.Status.REJECTED;
    };

    private final ResourceDAO resourceDAO;
    private final UserDAO userDAO;

    public BackupService() {
        this(new ResourceDAO(), new UserDAO());
    }

    public BackupService(ResourceDAO resourceDAO, UserDAO userDAO) {
        this.resourceDAO = resourceDAO;
        this.userDAO = userDAO;
    }

    private static boolean allowed(Class<?> type) {
        String name = type.getName();
        return name.equals("com.campus.exchange.backup.ResourceCatalogBackup")
                || name.equals("com.campus.exchange.backup.ResourceBackupItem")
                || name.equals("java.util.ArrayList")
                || name.equals("java.lang.String")
                || name.equals("java.math.BigDecimal")
                || name.equals("java.math.BigInteger")
                || name.equals("java.lang.Number")
                || name.equals("java.lang.Integer")
                || name.equals("java.lang.Long");
    }

    public String createBackup() {
        try {
            List<Resource> resources = resourceDAO.findAll();
            ResourceCatalogBackup backup = new ResourceCatalogBackup();
            for (Resource resource : resources) {
                backup.getItems().add(ResourceBackupItem.from(resource));
            }

            Path directory = AppConfig.backupDir();
            Files.createDirectories(directory);
            String filename = "catalog-" + LocalDateTime.now().format(FILE_TIME) + ".ser";
            Path target = directory.resolve(filename).normalize();
            if (!target.startsWith(directory)) {
                throw new ValidationException("Invalid backup path.");
            }
            writeBackup(target, backup);
            return filename;
        } catch (IOException e) {
            LOG.severe("Writing backup: " + e.getMessage());
            throw new AppException("The backup could not be written.");
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Loading resources for backup", e);
        }
    }

    public List<String> listBackups() {
        Path directory = AppConfig.backupDir();
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> SAFE_NAME.matcher(name).matches())
                    .sorted(Comparator.reverseOrder())
                    .toList();
        } catch (IOException e) {
            LOG.severe("Listing backups: " + e.getMessage());
            throw new AppException("Backups could not be listed.");
        }
    }

    public int restore(String filename) {
        if (filename == null || !SAFE_NAME.matcher(filename).matches()
                || !listBackups().contains(filename)) {
            throw new ValidationException("Choose a backup created by this application.");
        }

        Path directory = AppConfig.backupDir();
        Path path = directory.resolve(filename).normalize();
        if (!path.startsWith(directory)) {
            throw new ValidationException("Invalid backup path.");
        }

        ResourceCatalogBackup backup = readBackup(path);
        List<Resource> resources = validateBackup(backup);

        try (Connection conn = DbConnection.getConnection()) {
            conn.setAutoCommit(false);
            int inserted = 0;
            try {
                for (Resource resource : resources) {
                    if (resourceDAO.existsByOwnerAndTitle(
                            conn, resource.getOwnerId(), resource.getTitle())) {
                        continue;
                    }
                    resourceDAO.insert(conn, resource);
                    inserted++;
                }
                conn.commit();
                return inserted;
            } catch (AppException e) {
                rollbackQuietly(conn);
                throw e;
            } catch (SQLException e) {
                rollbackQuietly(conn);
                throw DbErrors.wrap(LOG, "Restoring backup", e);
            }
        } catch (SQLException e) {
            throw DbErrors.wrap(LOG, "Opening restore transaction", e);
        }
    }

    private List<Resource> validateBackup(ResourceCatalogBackup backup) {
        if (backup == null) {
            throw new ValidationException("The backup is empty or invalid.");
        }
        if (backup.getItems() == null || backup.getItems().size() > MAX_ITEMS) {
            throw new ValidationException("The backup contains an invalid number of listings.");
        }

        List<Resource> resources = new ArrayList<>();
        int index = 1;
        for (ResourceBackupItem item : backup.getItems()) {
            if (item == null) {
                throw new ValidationException("Backup item " + index + " is empty.");
            }
            try {
                if (userDAO.findById(item.getOwnerId()) == null) {
                    throw new ValidationException(
                            "Backup item " + index + " refers to a missing owner.");
                }
                Resource resource = ResourceService.newResource(item.parsedResourceType());
                resource.setOwnerId(item.getOwnerId());
                resource.setTitle(item.getTitle());
                resource.setDescription(item.getDescription());
                resource.setCategory(item.getCategory());
                resource.setResourceType(item.parsedResourceType());
                resource.setCondition(item.parsedCondition());
                resource.setExchangeType(item.parsedExchangeType());
                resource.setPrice(item.getPrice());
                resource.setLendingTerms(item.getLendingTerms());
                resource.setCourseCode(item.getCourseCode());
                resource.setPickupLocation(item.getPickupLocation());
                resource.setImagePath(item.getImagePath());
                ResourceStatus status = item.parsedStatus();
                resource.setStatus(status == ResourceStatus.INACTIVE
                        ? ResourceStatus.INACTIVE
                        : ResourceStatus.AVAILABLE);
                // Structure only: a backup must restore even if the business rules
                // changed since it was written (e.g. lending fees became mandatory).
                validateRestorable(resource);
                resources.add(resource);
            } catch (ValidationException e) {
                throw new ValidationException("Backup item " + index + ": " + e.getMessage());
            } catch (SQLException e) {
                throw DbErrors.wrap(LOG, "Validating backup owner", e);
            } catch (IllegalArgumentException e) {
                throw new ValidationException(
                        "Backup item " + index + " contains an unknown enum value.");
            }
            index++;
        }
        return resources;
    }

    /**
     * Checks that a stored row can be inserted again. Deliberately ignores the
     * current business rules so backups survive later rule changes.
     */
    private static void validateRestorable(Resource resource) {
        if (isBlank(resource.getTitle()) || isBlank(resource.getDescription())
                || isBlank(resource.getCategory()) || isBlank(resource.getPickupLocation())) {
            throw new ValidationException("required fields are missing.");
        }
        if (resource.getPrice() != null && resource.getPrice().signum() < 0) {
            throw new ValidationException("price must not be negative.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    ResourceCatalogBackup readBackup(Path path) {
        try (ObjectInputStream input = new ObjectInputStream(
                Files.newInputStream(path, StandardOpenOption.READ))) {
            input.setObjectInputFilter(FILTER);
            Object object = input.readObject();
            if (!(object instanceof ResourceCatalogBackup backup)) {
                throw new ValidationException("The selected file is not a catalogue backup.");
            }
            return backup;
        } catch (ValidationException e) {
            throw e;
        } catch (IOException | ClassNotFoundException | RuntimeException e) {
            LOG.warning("Rejected backup " + path.getFileName() + ": " + e.getMessage());
            throw new ValidationException(
                    "The backup is corrupt, incompatible, or contains disallowed data.");
        }
    }

    void writeBackup(Path path, ResourceCatalogBackup backup) throws IOException {
        try (ObjectOutputStream output = new ObjectOutputStream(
                Files.newOutputStream(path, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE))) {
            output.writeObject(backup);
        }
    }

    private void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException ignored) {
            // Preserve the original failure.
        }
    }
}
