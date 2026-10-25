#!/usr/bin/env python3
"""Generate the vector diagrams used in the project report.

The figures are hand-laid-out SVG so that they stay sharp in the PDF and can be
regenerated without a diagramming tool.  Run from this directory:

    python3 make_figures.py
"""

from __future__ import annotations

import html
import math
from pathlib import Path

INK = "#102a43"
SOFT = "#334e68"
MUTED = "#52677d"
NAVY = "#123a6b"
NAVY_DEEP = "#0b2749"
TEAL = "#0f766e"
LINE = "#8fa6bd"
PALE = "#f5f8fc"
PALE_BLUE = "#eaf1f9"
PALE_TEAL = "#e6f4f1"
WHITE = "#ffffff"
DANGER = "#9b2c2c"

SANS = "Liberation Sans"
MONO = "Liberation Mono"


def esc(value: str) -> str:
    return html.escape(value, quote=True)


class SVG:
    def __init__(self, width: int, height: int) -> None:
        self.width = width
        self.height = height
        self.parts: list[str] = []

    # -- primitives ---------------------------------------------------------
    def rect(self, x, y, w, h, fill=WHITE, stroke=LINE, sw=1.2, rx=8, dash=None,
             opacity=None):
        extra = f' stroke-dasharray="{dash}"' if dash else ""
        extra += f' opacity="{opacity}"' if opacity else ""
        self.parts.append(
            f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" '
            f'fill="{fill}" stroke="{stroke}" stroke-width="{sw}"{extra}/>'
        )

    def line(self, x1, y1, x2, y2, color=SOFT, sw=1.6, dash=None):
        extra = f' stroke-dasharray="{dash}"' if dash else ""
        self.parts.append(
            f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" '
            f'stroke="{color}" stroke-width="{sw}"{extra}/>'
        )

    def text(self, x, y, value, size=18, fill=INK, anchor="start",
             weight="normal", family=SANS, italic=False):
        style = ' font-style="italic"' if italic else ""
        self.parts.append(
            f'<text x="{x}" y="{y}" font-family="{family}" font-size="{size}" '
            f'font-weight="{weight}" fill="{fill}" text-anchor="{anchor}"'
            f'{style}>{esc(value)}</text>'
        )

    def polygon(self, points, fill=SOFT, stroke="none", sw=1):
        pts = " ".join(f"{x},{y}" for x, y in points)
        self.parts.append(
            f'<polygon points="{pts}" fill="{fill}" stroke="{stroke}" '
            f'stroke-width="{sw}"/>'
        )

    # -- composite ----------------------------------------------------------
    def box(self, x, y, w, h, title, sub=None, fill=WHITE, stroke=LINE,
            title_color=INK, sub_color=MUTED, title_size=21, sub_size=16,
            dash=None, sw=1.3):
        self.rect(x, y, w, h, fill=fill, stroke=stroke, sw=sw, dash=dash)
        if sub:
            lines = sub.split("\n")
            block = title_size + 6 + len(lines) * (sub_size + 4)
            top = y + (h - block) / 2
            self.text(x + w / 2, top + title_size, title, title_size,
                      title_color, "middle", "bold")
            for index, line in enumerate(lines):
                self.text(x + w / 2,
                          top + title_size + 6 + (index + 1) * (sub_size + 2),
                          line, sub_size, sub_color, "middle")
        else:
            self.text(x + w / 2, y + h / 2 + title_size * 0.35, title,
                      title_size, title_color, "middle", "bold")

    def entity(self, x, y, w, title, rows, header=NAVY, row_size=16,
               header_h=30, row_h=20):
        h = header_h + len(rows) * row_h + 10
        self.rect(x, y, w, h, fill=WHITE, stroke=LINE, sw=1.3, rx=6)
        self.parts.append(
            f'<path d="M{x + 6},{y} H{x + w - 6} A6,6 0 0 1 {x + w},{y + 6} '
            f'V{y + header_h} H{x} V{y + 6} A6,6 0 0 1 {x + 6},{y} Z" '
            f'fill="{header}" stroke="none"/>'
        )
        self.text(x + w / 2, y + header_h - 9, title, 19, WHITE, "middle",
                  "bold", MONO)
        ty = y + header_h + row_size
        for row in rows:
            self.text(x + 10, ty, row, row_size, SOFT, "start", "normal", MONO)
            ty += row_h
        return h

    def arrow(self, x1, y1, x2, y2, color=SOFT, sw=1.6, dash=None,
              head=9, label=None, label_pos=0.5, label_size=16,
              label_color=SOFT, label_bg=True):
        ang = math.atan2(y2 - y1, x2 - x1)
        # Stop the shaft short of the head so the outline stays clean.
        ex = x2 - math.cos(ang) * head * 0.85
        ey = y2 - math.sin(ang) * head * 0.85
        extra = f' stroke-dasharray="{dash}"' if dash else ""
        self.parts.append(
            f'<line x1="{x1}" y1="{y1}" x2="{ex}" y2="{ey}" stroke="{color}" '
            f'stroke-width="{sw}"{extra}/>'
        )
        left = ang + math.radians(150)
        right = ang - math.radians(150)
        self.polygon([
            (x2, y2),
            (x2 + math.cos(left) * head, y2 + math.sin(left) * head),
            (x2 + math.cos(right) * head, y2 + math.sin(right) * head),
        ], fill=color)
        if label:
            lx = x1 + (x2 - x1) * label_pos
            ly = y1 + (y2 - y1) * label_pos - 6
            self.label(lx, ly, label, label_size, label_color, label_bg)

    def label(self, x, y, value, size=16, color=SOFT, background=True,
              anchor="middle"):
        if background:
            width = len(value) * size * 0.56 + 8
            bx = x - width / 2 if anchor == "middle" else x - 4
            self.rect(bx, y - size + 1, width, size + 5, fill=WHITE,
                      stroke="none", sw=0, rx=2)
        self.text(x, y, value, size, color, anchor)

    def polyline(self, points, color=SOFT, sw=1.5, dash=None, head=True):
        extra = f' stroke-dasharray="{dash}"' if dash else ""
        path = " ".join(f"{x},{y}" for x, y in points)
        self.parts.append(
            f'<polyline points="{path}" fill="none" stroke="{color}" '
            f'stroke-width="{sw}" stroke-linejoin="round"{extra}/>'
        )
        if head and len(points) >= 2:
            (x1, y1), (x2, y2) = points[-2], points[-1]
            ang = math.atan2(y2 - y1, x2 - x1)
            left = ang + math.radians(150)
            right = ang - math.radians(150)
            h = 8
            self.polygon([
                (x2, y2),
                (x2 + math.cos(left) * h, y2 + math.sin(left) * h),
                (x2 + math.cos(right) * h, y2 + math.sin(right) * h),
            ], fill=color)

    def many(self, x, y, direction, color=SOFT, sw=1.5):
        """Crow's-foot marker pointing at (x, y). direction: l/r/u/d."""
        angles = {"l": 180, "r": 0, "u": 90, "d": 270}[direction]
        base = math.radians(angles)
        for offset in (-28, 0, 28):
            a = base + math.radians(offset)
            self.line(x, y, x + math.cos(a) * 11, y + math.sin(a) * 11,
                      color, sw)

    def save(self, path: Path, crop_top: int = 0, crop_bottom: int = 0):
        height = self.height - crop_top - crop_bottom
        body = "\n".join(self.parts)
        doc = (
            f'<svg xmlns="http://www.w3.org/2000/svg" width="{self.width}" '
            f'height="{height}" viewBox="0 {crop_top} {self.width} {height}">'
            f'<rect width="{self.width}" height="{self.height}" fill="{WHITE}"/>'
            f'{body}</svg>'
        )
        path.write_text(doc, encoding="utf-8")
        print(f"wrote {path} ({len(doc) // 1024} KiB)")


def band(svg: SVG, x, y, w, h, title, subtitle, color):
    svg.rect(x, y, w, h, fill=PALE, stroke="#dbe5ef", sw=1.0, rx=8)
    svg.rect(x, y, 6, h, fill=color, stroke="none", sw=0, rx=3)
    svg.text(x + 16, y + 26, title, 18, color, "start", "bold")
    svg.text(x + 16, y + 46, subtitle, 14, MUTED)


def figure_architecture(out: Path):
    svg = SVG(1000, 700)
    def band(y, h, title, subtitle, color):
        svg.rect(150, y, 820, h, fill=PALE, stroke="#dbe5ef", sw=1.0, rx=8)
        svg.rect(150, y, 6, h, fill=color, stroke="none", sw=0, rx=3)
        svg.text(22, y + 27, title, 14, color, "start", "bold")
        svg.text(22, y + 45, subtitle, 10.5, MUTED, "start")

    band(40, 85, "Presentation", "browser and views", NAVY)
    svg.box(165, 54, 350, 58, "Browser", "student or administrator", PALE_BLUE,
            NAVY)
    svg.box(545, 54, 410, 58, "JSP 3.1 + JSTL views",
            "escaped output · no scriptlets", PALE_BLUE, NAVY)

    band(145, 100, "Web", "controllers", TEAL)
    svg.box(165, 160, 275, 70, "SecurityFilter",
            "session · roles · CSRF · headers", PALE_TEAL, TEAL)
    svg.box(465, 160, 490, 70, "Servlet controllers (10)",
            "routing · validation · flash", PALE_TEAL, TEAL)

    band(265, 85, "Business", "services", NAVY)
    svg.box(165, 279, 790, 58, "Services",
            "Auth · Resource · Request · Transaction · Review · Admin · Backup",
            PALE_BLUE, NAVY)

    band(370, 95, "Data access", "JDBC and DAOs", TEAL)
    svg.box(165, 384, 510, 66, "DAO layer (5 DAOs)",
            "PreparedStatement · row mapping", PALE_TEAL, TEAL)
    svg.box(700, 384, 255, 66, "JDBC", "connections · locks", PALE_TEAL, TEAL)

    band(485, 140, "Storage", "MySQL and files", NAVY)
    svg.box(165, 500, 320, 110, "MySQL 8",
            "users · resources · requests\ntransactions · reviews", PALE_BLUE, NAVY)
    svg.box(510, 500, 210, 110, "Upload directory",
            "JPEG / PNG / GIF\nUUID filenames", PALE_BLUE, NAVY)
    svg.box(745, 500, 210, 110, "Backup directory",
            "catalog-*.ser\nObjectInputFilter", PALE_BLUE, NAVY)

    svg.arrow(310, 112, 310, 160, MUTED, 1.6, label="HTTP request",
              label_size=13)
    svg.arrow(880, 160, 880, 112, MUTED, 1.6, dash="4 4", label="HTML",
              label_size=13)
    svg.arrow(510, 230, 510, 279, MUTED, 1.6, label="service calls",
              label_size=13)
    svg.arrow(510, 337, 510, 384, MUTED, 1.6, label="DAO calls",
              label_size=13)
    svg.arrow(330, 450, 330, 500, MUTED, 1.6, label="SQL", label_size=13)

    svg.rect(165, 655, 790, 28, fill="#eef2f7", stroke="none", sw=0, rx=5)
    svg.text(175, 674,
             "AppConfig — DB_URL · DB_USER · DB_PASSWORD · UPLOAD_DIR · BACKUP_DIR",
             12.5, SOFT, "start", MONO)
    svg.save(out, crop_top=18)


def figure_domain(out: Path):
    svg = SVG(1000, 760)

    # User hierarchy
    svg.rect(50, 100, 270, 170, fill=WHITE, stroke=LINE, rx=6)
    svg.text(60, 126, "«abstract» User", 20, NAVY, "start", "bold")
    for i, line in enumerate([
        "- userId : int",
        "- fullName, email : String",
        "- passwordHash : String",
        "- role, verificationStatus",
        "+ getDashboardPath()",
        "+ getRoleLabel()",
    ]):
        svg.text(60, 150 + i * 20, line, 14, SOFT, "start", "normal", MONO)

    svg.rect(30, 350, 200, 95, fill=PALE_BLUE, stroke=LINE, rx=6)
    svg.text(130, 376, "Student", 19, NAVY, "middle", "bold")
    svg.text(130, 398, "+ canListResources()", 13.5, SOFT, "middle", "normal", MONO)
    svg.text(130, 416, "+ canSubmitRequests()", 13.5, SOFT, "middle", "normal", MONO)

    svg.rect(250, 350, 160, 95, fill=PALE_BLUE, stroke=LINE, rx=6)
    svg.text(330, 376, "Admin", 19, NAVY, "middle", "bold")
    svg.text(330, 400, "+ getDashboardPath()", 12, SOFT, "middle", "normal", MONO)
    svg.text(330, 418, "+ getRoleLabel()", 12, SOFT, "middle", "normal", MONO)

    for x in (130, 330):
        svg.line(x, 350, x, 284)
        svg.polygon([(x, 270), (x - 8, 284), (x + 8, 284)], fill=WHITE,
                    stroke=NAVY, sw=1.3)

    # Resource hierarchy
    svg.rect(520, 100, 430, 190, fill=WHITE, stroke=LINE, rx=6)
    svg.text(530, 126, "«abstract» Resource", 20, NAVY, "start", "bold")
    for i, line in enumerate([
        "- resourceId, ownerId : int",
        "- title, description, category",
        "- resourceType, condition",
        "- exchangeType, price, status",
        "+ displayDetails() : String",
        "+ matches(query) : boolean",
    ]):
        svg.text(530, 150 + i * 20, line, 14, SOFT, "start", "normal", MONO)

    for x, name, w in ((515, "Book", 130), (665, "LabEquipment", 155),
                       (835, "StudyMaterial", 145)):
        svg.rect(x, 380, w, 95, fill=PALE_BLUE, stroke=LINE, rx=6)
        svg.text(x + w / 2, 414, name, 17, NAVY, "middle", "bold")
        svg.text(x + w / 2, 440, "+ displayDetails()", 12, SOFT, "middle",
                 "normal", MONO)
        svg.line(x + w / 2, 380, x + w / 2, 304)
        svg.polygon([(x + w / 2, 290), (x + w / 2 - 8, 304),
                     (x + w / 2 + 8, 304)], fill=WHITE, stroke=NAVY, sw=1.3)

    # Interfaces
    svg.rect(865, 25, 110, 55, fill=PALE_TEAL, stroke=TEAL, rx=6, dash="5 3")
    svg.text(920, 45, "«interface»", 10, TEAL, "middle")
    svg.text(920, 64, "Searchable", 14, TEAL, "middle", "bold")
    svg.line(920, 100, 920, 80, TEAL, 1.4, dash="5 3")

    svg.rect(575, 500, 290, 52, fill=PALE_TEAL, stroke=TEAL, rx=6, dash="5 3")
    svg.text(720, 519, "«interface» Sellable / Lendable", 14, TEAL, "middle",
             "bold")
    svg.text(720, 538, "getSalePrice() · getLoanPeriodDays()", 10, TEAL,
             "middle")
    for x in (515, 720, 835):
        svg.line(x, 475, x, 490, TEAL, 1.3, dash="5 3")
    svg.line(515, 490, 835, 490, TEAL, 1.3, dash="5 3")
    svg.line(720, 490, 720, 500, TEAL, 1.3, dash="5 3")

    # Associations
    svg.line(320, 165, 520, 165, MUTED, 1.4)
    svg.label(330, 157, "1", 12, MUTED, False)
    svg.label(505, 157, "0..*", 12, MUTED, False)
    svg.text(420, 157, "owns", 12, MUTED, "middle")

    svg.polyline([(50, 230), (18, 230), (18, 630), (50, 630)], MUTED, 1.4)

    svg.polyline([(520, 250), (420, 250), (420, 530), (300, 530), (300, 560)],
                 MUTED, 1.4)
    svg.label(470, 242, "1", 12, MUTED, False)
    svg.label(310, 548, "0..*", 12, MUTED, False)

    # Lower entities
    svg.rect(50, 560, 270, 150, fill=WHITE, stroke=LINE, rx=6)
    svg.text(60, 586, "ResourceRequest", 19, NAVY, "start", "bold")
    for i, line in enumerate(["- requestId, resourceId", "- requesterId, message",
                              "- status : RequestStatus",
                              "+ isPending(), isAccepted()"]):
        svg.text(60, 610 + i * 20, line, 12.5, SOFT, "start", "normal", MONO)

    svg.rect(370, 560, 290, 150, fill=WHITE, stroke=LINE, rx=6)
    svg.text(380, 586, "Transaction", 19, NAVY, "start", "bold")
    for i, line in enumerate(["- transactionId, requestId", "- ownerId, requesterId",
                              "- status : TransactionStatus",
                              "+ isCompleted(), isLend()"]):
        svg.text(380, 610 + i * 20, line, 12.5, SOFT, "start", "normal", MONO)

    svg.rect(710, 560, 250, 140, fill=WHITE, stroke=LINE, rx=6)
    svg.text(720, 586, "Review", 19, NAVY, "start", "bold")
    for i, line in enumerate(["- reviewId, transactionId", "- reviewerId, rating",
                              "- comment, createdAt"]):
        svg.text(720, 610 + i * 20, line, 12.5, SOFT, "start", "normal", MONO)

    svg.arrow(320, 630, 370, 630, MUTED, 1.4)
    svg.label(345, 620, "0..1", 12, MUTED)
    svg.arrow(660, 630, 710, 630, MUTED, 1.4)
    svg.label(685, 620, "0..1", 12, MUTED)

    svg.save(out)


def figure_er(out: Path):
    svg = SVG(1000, 700)
    svg.entity(30, 45, 250, "users", [
        "PK  user_id",
        "UQ  email",
        "     password_hash",
        "UQ  student_id",
        "     role",
        "     verification_status",
        "     is_active",
    ])
    svg.entity(350, 45, 280, "resources", [
        "PK  resource_id",
        "FK  owner_id",
        "     title, category",
        "     resource_type",
        "     condition",
        "     exchange_type",
        "     price, status",
        "     image_path",
        "     created_at",
    ], header=TEAL)
    svg.entity(30, 370, 250, "resource_requests", [
        "PK  request_id",
        "FK  resource_id",
        "FK  requester_id",
        "     message",
        "     status",
        "     created_at",
    ], header=NAVY)
    svg.entity(350, 370, 290, "exchange_transactions", [
        "PK  transaction_id",
        "FK  request_id  (unique)",
        "FK  owner_id",
        "FK  requester_id",
        "     exchange_type",
        "     status",
        "     completed_at",
        "     returned_at",
    ], header=TEAL)
    svg.entity(710, 370, 260, "reviews", [
        "PK  review_id",
        "FK  transaction_id  (unique)",
        "FK  reviewer_id",
        "     rating  (1–5)",
        "     comment",
        "     created_at",
    ], header=NAVY)

    def edge(points, one_label, many_label, many_dir, many_xy):
        svg.polyline(points, MUTED, 1.4, head=False)
        svg.many(many_xy[0], many_xy[1], many_dir)
        svg.label(points[0][0] + 14, points[0][1] - 8, one_label, 13, MUTED, False)
        svg.label(many_xy[0] - 12, many_xy[1] - 8, many_label, 13, MUTED, False)

    # users -> resources
    edge([(280, 130), (350, 130)], "1", "0..*", "r", (350, 130))
    # users -> requests
    edge([(155, 225), (155, 370)], "1", "0..*", "d", (155, 370))
    # resources -> requests
    svg.polyline([(490, 265), (490, 320), (280, 320), (280, 370)], MUTED, 1.4,
                 head=False)
    svg.many(280, 370, "d")
    svg.label(500, 305, "1", 13, MUTED, False)
    svg.label(268, 356, "0..*", 13, MUTED, False)
    # requests -> transactions (1 : 0..1)
    svg.line(280, 455, 350, 455, MUTED, 1.4)
    svg.label(300, 447, "1", 13, MUTED, False)
    svg.label(338, 447, "0..1", 13, MUTED, False)
    # transactions -> reviews
    svg.line(640, 455, 710, 455, MUTED, 1.4)
    svg.label(655, 447, "1", 13, MUTED, False)
    svg.label(692, 447, "0..1", 13, MUTED, False)

    svg.text(20, 665,
             "All foreign keys use ON DELETE RESTRICT; unique keys enforce one "
             "transaction per request and one review per transaction.",
             14, MUTED, "start")
    svg.save(out, crop_top=30)


def figure_states(out: Path):
    svg = SVG(1000, 780)

    def lane(y, h, title, subtitle):
        svg.rect(20, y, 960, h, fill=PALE, stroke="#dbe5ef", rx=8)
        svg.text(36, y + 26, title, 19, NAVY, "start", "bold")
        svg.text(36, y + 46, subtitle, 14, MUTED, "start")

    def state(x, y, label, w=170, h=54, fill=PALE_BLUE):
        svg.rect(x, y, w, h, fill=fill, stroke=NAVY, rx=27)
        svg.text(x + w / 2, y + h / 2 + 6, label, 18, NAVY, "middle", "bold")

    lane(30, 280, "Resource", "resources.status")
    state(50, 100, "AVAILABLE")
    state(320, 100, "RESERVED")
    state(650, 55, "UNAVAILABLE")
    state(650, 165, "LENT")
    state(50, 210, "INACTIVE")
    svg.arrow(220, 127, 320, 127, MUTED, 1.5, label="accept", label_size=14)
    svg.arrow(490, 114, 650, 82, MUTED, 1.5, label="handover", label_size=14,
              label_pos=0.5)
    svg.arrow(490, 146, 650, 192, MUTED, 1.5, label="loan start", label_size=14)
    svg.polyline([(650, 192), (560, 192), (560, 285), (240, 285), (240, 127),
                  (220, 127)], MUTED, 1.5)
    svg.label(400, 278, "return", 14, MUTED)
    svg.arrow(135, 154, 135, 210, MUTED, 1.5)
    svg.text(148, 180, "deactivate", 12.5, MUTED, "start")

    lane(330, 210, "Request", "resource_requests.status")
    state(50, 390, "PENDING")
    state(300, 390, "ACCEPTED")
    state(650, 390, "COMPLETED")
    state(300, 470, "REJECTED")
    state(50, 470, "CANCELLED")
    svg.arrow(220, 417, 300, 417, MUTED, 1.5, label="owner accepts", label_size=13)
    svg.arrow(470, 417, 650, 417, MUTED, 1.5, label="exchange ends", label_size=13)
    svg.arrow(135, 444, 135, 470, MUTED, 1.5, label="cancel", label_size=13,
              label_pos=0.45)
    svg.arrow(220, 430, 330, 470, MUTED, 1.5, label="reject", label_size=13)

    lane(560, 210, "Transaction", "exchange_transactions.status")
    state(40, 615, "PENDING_HANDOVER", w=210)
    state(360, 660, "ACTIVE_LOAN", w=180)
    state(650, 615, "COMPLETED")
    svg.arrow(250, 630, 650, 630, MUTED, 1.5,
              label="sale / donation handover", label_size=13)
    svg.arrow(250, 650, 360, 685, MUTED, 1.5, label="loan starts", label_size=13)
    svg.arrow(540, 675, 650, 650, MUTED, 1.5, label="return", label_size=13)

    svg.save(out, crop_bottom=30)


def figure_sequence(out: Path):
    svg = SVG(1000, 690)
    actors = [
        (70, "Owner", "browser"),
        (230, "SecurityFilter", "filter"),
        (400, "RequestServlet", "controller"),
        (570, "RequestService", "service"),
        (750, "DAOs", "data access"),
        (920, "MySQL", "database"),
    ]
    for x, name, kind in actors:
        svg.rect(x - 62, 40, 124, 50, fill=PALE_BLUE, stroke=NAVY, rx=6)
        svg.text(x, 60, name, 14, NAVY, "middle", "bold")
        svg.text(x, 78, kind, 11, MUTED, "middle")
        svg.line(x, 90, x, 720, LINE, 1.0, dash="4 5")

    def message(y, src, dst, text, dashed=False):
        color = MUTED if not dashed else TEAL
        svg.text(20, y + 4, f"{message.n}.", 10, MUTED, "start")
        svg.arrow(src, y, dst, y, color, 1.4, dash="5 4" if dashed else None,
                  label=text, label_size=13, label_pos=0.5, label_color=INK)
        message.n += 1

    message.n = 1
    message(130, 70, 230, "POST /requests/accept (id, _csrf)")
    message(172, 230, 400, "chain.doFilter (session, role, CSRF ok)")
    message(214, 400, 570, "acceptRequest(requestId, ownerId)")
    message(256, 570, 920, "BEGIN")
    message(298, 570, 920, "SELECT request … FOR UPDATE")
    message(340, 570, 920, "SELECT resource … FOR UPDATE")
    svg.rect(585, 365, 58, 30, fill=WHITE, stroke=TEAL, rx=4)
    svg.text(614, 385, "guards", 11, TEAL, "middle")
    svg.line(570, 372, 585, 372, TEAL, 1.3)
    svg.line(643, 388, 658, 388, TEAL, 1.3)
    svg.text(664, 393, "owner · PENDING · AVAILABLE", 12, TEAL, "start")
    message(430, 570, 920, "UPDATE resource → RESERVED")
    message(472, 570, 920, "UPDATE request → ACCEPTED")
    message(514, 570, 920, "INSERT transaction")
    message(556, 570, 920, "COMMIT")
    message(610, 570, 400, "transactionId", dashed=True)
    message(652, 400, 70, "302 /transactions/view?id=…", dashed=True)

    svg.save(out, crop_top=28)


def main():
    here = Path(__file__).resolve().parent
    figures = here / "figures"
    figures.mkdir(exist_ok=True)
    figure_architecture(figures / "fig1-architecture.svg")
    figure_domain(figures / "fig2-domain.svg")
    figure_er(figures / "fig3-er.svg")
    figure_states(figures / "fig4-states.svg")
    figure_sequence(figures / "fig5-sequence.svg")


if __name__ == "__main__":
    main()
