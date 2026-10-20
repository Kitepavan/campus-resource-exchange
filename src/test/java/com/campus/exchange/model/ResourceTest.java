package com.campus.exchange.model;

import com.campus.exchange.exception.ValidationException;
import com.campus.exchange.model.enums.Condition;
import com.campus.exchange.model.enums.ExchangeType;
import com.campus.exchange.model.enums.ResourceStatus;
import com.campus.exchange.model.enums.ResourceType;
import com.campus.exchange.service.ResourceService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ResourceTest {

    @Test
    void validSellBookIsNormalized() {
        Book book = new Book();
        book.setTitle("Clean Code");
        book.setDescription("A very clean copy with no notes.");
        book.setCategory("Computer Science");
        book.setResourceType(ResourceType.BOOK);
        book.setCondition(Condition.GOOD);
        book.setExchangeType(ExchangeType.SELL);
        book.setPrice(new BigDecimal("45.00"));
        book.setPickupLocation("Central Library");

        ResourceService.validate(book);

        assertEquals(new BigDecimal("45.00"), book.getPrice());
        assertNull(book.getLendingTerms());
        assertTrue(book instanceof Sellable);
        assertTrue(book instanceof Lendable);
        assertEquals("Book: Clean Code", book.displayDetails());
        assertEquals(30, ((Lendable) book).getLoanPeriodDays());
    }

    @Test
    void validLendLabEquipmentRequiresTerms() {
        LabEquipment equipment = new LabEquipment();
        equipment.setTitle("Arduino Uno Kit");
        equipment.setDescription("Board, breadboard, cables, and LEDs.");
        equipment.setCategory("Electronics");
        equipment.setResourceType(ResourceType.LAB_EQUIPMENT);
        equipment.setCondition(Condition.GOOD);
        equipment.setExchangeType(ExchangeType.LEND);
        equipment.setPrice(new BigDecimal("0.00"));
        equipment.setLendingTerms("7 days, return all parts");
        equipment.setPickupLocation("Electronics Lab");

        ResourceService.validate(equipment);

        assertNull(equipment.getPrice());
        assertEquals("7 days, return all parts", equipment.getLendingTerms());
        assertEquals("Lab equipment: Arduino Uno Kit", equipment.displayDetails());
        assertEquals(7, ((Lendable) equipment).getLoanPeriodDays());
    }

    @Test
    void donateIsAlwaysFree() {
        StudyMaterial material = new StudyMaterial();
        material.setTitle("Calculus Notes");
        material.setDescription("Full semester notes with examples.");
        material.setCategory("Mathematics");
        material.setResourceType(ResourceType.STUDY_MATERIAL);
        material.setCondition(Condition.FAIR);
        material.setExchangeType(ExchangeType.DONATE);
        material.setPrice(new BigDecimal("10.00"));
        material.setPickupLocation("Science Block");

        ResourceService.validate(material);

        assertNull(material.getPrice());
        assertEquals("Study material: Calculus Notes", material.displayDetails());
    }

    @Test
    void sellRequiresPositivePrice() {
        Book book = validBook();
        book.setExchangeType(ExchangeType.SELL);
        book.setPrice(BigDecimal.ZERO);

        ValidationException error = assertThrows(ValidationException.class,
                () -> ResourceService.validate(book));
        assertTrue(error.getMessage().contains("Price"));
    }

    @Test
    void lendRequiresTerms() {
        Book book = validBook();
        book.setExchangeType(ExchangeType.LEND);
        book.setLendingTerms(null);

        assertThrows(ValidationException.class, () -> ResourceService.validate(book));
    }

    @Test
    void requiredFieldsAreValidated() {
        Book book = validBook();
        book.setTitle("ab");

        assertThrows(ValidationException.class, () -> ResourceService.validate(book));

        Book missingDescription = validBook();
        missingDescription.setDescription("short");
        assertThrows(ValidationException.class, () -> ResourceService.validate(missingDescription));
    }

    @Test
    void searchMatchesKeywordCategoryAndCourse() {
        Book book = validBook();
        book.setTitle("Introduction to Algorithms");
        book.setCategory("Computer Science");
        book.setCourseCode("CS301");

        assertTrue(book.matches("algorithms"));
        assertTrue(book.matches("computer"));
        assertTrue(book.matches("cs301"));
        assertFalse(book.matches("biology"));
        assertTrue(book.matches(""));
    }

    @Test
    void resourceStatusHelpersWork() {
        Book book = validBook();
        book.setStatus(ResourceStatus.AVAILABLE);
        assertTrue(book.isAvailable());
        assertFalse(book.isReserved());

        book.setStatus(ResourceStatus.LENT);
        assertTrue(book.isLent());
        assertFalse(book.isAvailable());
    }

    private Book validBook() {
        Book book = new Book();
        book.setTitle("A Valid Title");
        book.setDescription("This description is long enough to pass validation.");
        book.setCategory("General");
        book.setResourceType(ResourceType.BOOK);
        book.setCondition(Condition.NEW);
        book.setExchangeType(ExchangeType.SELL);
        book.setPrice(BigDecimal.ONE);
        book.setPickupLocation("Main Library");
        return book;
    }
}
