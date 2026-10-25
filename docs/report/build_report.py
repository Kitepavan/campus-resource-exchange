#!/usr/bin/env python3
"""Build the PDF report from report.md.

Usage (from this directory):

    ../../.report-venv/bin/python build_report.py

The script converts the Markdown source to HTML, generates a table of contents
with live page references, and renders the result with WeasyPrint.
"""

from __future__ import annotations

import html
import re
from pathlib import Path

import markdown
from weasyprint import HTML

HERE = Path(__file__).resolve().parent
SOURCE = HERE / "report.md"
PDF = HERE / "Campus_Resource_Exchange_Report.pdf"
HTML_OUT = HERE / "report.html"

META_RE = re.compile(r"\A---\n(.*?)\n---\n", re.S)


def parse_source(text: str):
    match = META_RE.match(text)
    if not match:
        raise SystemExit("report.md must start with a YAML-style metadata block")
    meta = {}
    for line in match.group(1).splitlines():
        if ":" in line:
            key, value = line.split(":", 1)
            meta[key.strip()] = value.strip()
    return meta, text[match.end():]


def slugify(value: str) -> str:
    value = re.sub(r"<[^>]+>", "", value)
    value = re.sub(r"[^\w\s.-]", "", value, flags=re.UNICODE)
    value = re.sub(r"[\s.]+", "-", value.strip()).strip("-")
    return value.lower() or "section"


def build_toc(tokens, level: int = 2) -> str:
    items = []
    for token in tokens:
        if token["level"] < 2 or token["level"] > 3:
            continue
        cls = "toc-l2" if token["level"] == 2 else "toc-l3"
        items.append(
            f'<li class="{cls}"><a href="#{token["id"]}">'
            f'{html.escape(token["name"])}</a></li>'
        )
        if token.get("children"):
            nested = build_toc(token["children"], level + 1)
            if nested:
                items.append(nested)
    if not items:
        return ""
    return "<ul>" + "".join(items) + "</ul>"


def title_page(meta: dict) -> str:
    authors = [a.strip() for a in meta.get("authors", "").split(",") if a.strip()]
    author_html = "".join(f"<span>{html.escape(a)}</span>" for a in authors)
    return f"""
<section class="title-page">
  <div class="title-top">
    <div class="title-mark">CR</div>
    <div class="title-kicker">{html.escape(meta.get('course', 'Course Project'))}</div>
  </div>

  <div class="title-main">
    <p class="title-eyebrow">Project Report</p>
    <h1>{html.escape(meta.get('title', 'Project Report'))}</h1>
    <p class="title-subtitle">{html.escape(meta.get('subtitle', ''))}</p>
    <div class="title-rule"></div>
    <p class="title-note">
      A report on the design, implementation, and evaluation of a
      database-backed peer-to-peer academic resource exchange.
    </p>
  </div>

  <div class="title-meta">
    <div class="meta-block">
      <span class="meta-label">Prepared by</span>
      <p class="meta-authors">{author_html}</p>
    </div>
    <div class="meta-block">
      <span class="meta-label">Date</span>
      <p>{html.escape(meta.get('date', ''))}</p>
    </div>
    <div class="meta-block">
      <span class="meta-label">Version</span>
      <p>{html.escape(meta.get('version', '1.0'))}</p>
    </div>
  </div>

  <div class="title-footer">
    <span>Jakarta EE</span>
    <span>JDBC</span>
    <span>MySQL 8</span>
    <span>Tomcat 10.1</span>
  </div>
</section>
"""


CSS = r"""
@page {
  size: A4;
  margin: 22mm 22mm 20mm 22mm;
  @top-left {
    content: "Campus Resource Exchange";
    font-family: "Liberation Sans", sans-serif;
    font-size: 7.8pt;
    color: #627d98;
    letter-spacing: 0.02em;
  }
  @top-right {
    content: "Project Report";
    font-family: "Liberation Sans", sans-serif;
    font-size: 7.8pt;
    color: #627d98;
    letter-spacing: 0.02em;
  }
  @bottom-center {
    content: counter(page);
    font-family: "Liberation Sans", sans-serif;
    font-size: 8.5pt;
    color: #334e68;
  }
}

@page :first {
  margin: 0;
  @top-left { content: none; }
  @top-right { content: none; }
  @bottom-center { content: none; }
}

html {
  font-family: "Liberation Serif", "DejaVu Serif", serif;
  font-size: 10.4pt;
  line-height: 1.52;
  color: #102a43;
}

body { margin: 0; }

p {
  margin: 0 0 0.55em;
  text-align: left;
  orphans: 3;
  widows: 3;
  hyphens: manual;
}

a { color: #0b5c56; text-decoration: none; }

h1, h2, h3, h4 {
  font-family: "Liberation Sans", "DejaVu Sans", sans-serif;
  color: #0b2749;
  line-height: 1.2;
  break-after: avoid;
}

h2 {
  font-size: 16.5pt;
  margin: 0 0 7mm;
  padding-bottom: 2.2mm;
  border-bottom: 1.2pt solid #0f766e;
  break-before: page;
}

h2.toc-title {
  break-before: avoid;
}

h3 {
  font-size: 11.6pt;
  margin: 6mm 0 2.2mm;
  color: #123a6b;
}

h4 { font-size: 10.5pt; margin: 4mm 0 1.5mm; }

strong { color: #0b2749; }

/* ---------- title page ---------- */

.title-page {
  height: 297mm;
  padding: 30mm 26mm 22mm;
  box-sizing: border-box;
  position: relative;
  page-break-after: always;
  background: #ffffff;
}

.title-page::before {
  content: "";
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 7mm;
  background: #0b2749;
}

.title-page::after {
  content: "";
  position: absolute;
  top: 7mm;
  left: 0;
  width: 34mm;
  height: 2.2mm;
  background: #0f766e;
}

.title-top {
  display: flex;
  align-items: center;
  gap: 6mm;
  margin-top: 8mm;
}

.title-mark {
  width: 15mm;
  height: 15mm;
  border-radius: 3mm;
  background: #123a6b;
  color: #ffffff;
  font-family: "Liberation Sans", sans-serif;
  font-size: 13pt;
  font-weight: bold;
  letter-spacing: 0.06em;
  display: flex;
  align-items: center;
  justify-content: center;
}

.title-kicker {
  font-family: "Liberation Sans", sans-serif;
  font-size: 9pt;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #52677d;
}

.title-main { margin-top: 48mm; }

.title-eyebrow {
  font-family: "Liberation Sans", sans-serif;
  font-size: 9.5pt;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #0f766e;
  font-weight: bold;
  margin-bottom: 6mm;
  text-align: left;
}

.title-main h1 {
  font-size: 31pt;
  letter-spacing: -0.02em;
  margin: 0 0 4mm;
  max-width: 150mm;
  line-height: 1.1;
}

.title-subtitle {
  hyphens: manual;
  font-size: 14pt;
  line-height: 1.35;
  color: #334e68;
  max-width: 138mm;
  text-align: left;
  margin-bottom: 9mm;
}

.title-rule {
  width: 46mm;
  height: 1.4mm;
  background: #0f766e;
  margin-bottom: 8mm;
}

.title-note {
  font-size: 10pt;
  color: #52677d;
  max-width: 118mm;
  text-align: left;
}

.title-meta {
  position: absolute;
  left: 26mm;
  right: 26mm;
  bottom: 44mm;
  display: flex;
  gap: 12mm;
  padding-top: 6mm;
  border-top: 0.6pt solid #bcccdc;
}

.meta-block { flex: 1; }

.meta-label {
  display: block;
  font-family: "Liberation Sans", sans-serif;
  font-size: 7.8pt;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #627d98;
  margin-bottom: 2mm;
}

.meta-block p {
  margin: 0;
  font-size: 10.5pt;
  text-align: left;
}

.meta-authors span {
  display: block;
  line-height: 1.5;
}

.title-footer {
  position: absolute;
  left: 26mm;
  right: 26mm;
  bottom: 22mm;
  font-family: "Liberation Sans", sans-serif;
  font-size: 8pt;
  color: #829ab1;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  display: flex;
  gap: 8mm;
}

/* ---------- table of contents ---------- */

.toc-page { break-before: page; }

.toc-page h2 {
  border-bottom: 1.2pt solid #0f766e;
  margin-bottom: 6mm;
}

.toc-page ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.toc-page li { margin: 0; }

.toc-page li a {
  display: block;
  color: #102a43;
  font-family: "Liberation Sans", sans-serif;
  font-size: 9.4pt;
  line-height: 1.45;
  padding: 1.1mm 0;
  border-bottom: 0.4pt dotted #c7d4e2;
}

.toc-page li a::after {
  content: target-counter(attr(href), page);
  float: right;
  color: #52677d;
  font-size: 8.8pt;
}

.toc-page .toc-l2 > a {
  font-weight: bold;
  color: #0b2749;
  margin-top: 2.2mm;
}

.toc-page .toc-l3 { padding-left: 7mm; }

.toc-page .toc-l3 > a {
  font-weight: normal;
  color: #334e68;
  font-size: 9pt;
}

/* ---------- body ---------- */

.mono, code {
  font-family: "Liberation Mono", "DejaVu Sans Mono", monospace;
  font-size: 0.88em;
}

p code, li code, td code {
  background: #eef3f8;
  padding: 0 0.6mm;
  border-radius: 1.2mm;
  color: #0b3d5c;
}

ul, ol {
  margin: 0 0 0.6em 5mm;
  padding: 0;
}

li {
  margin-bottom: 0.8mm;
  text-align: left;
}

li p { margin-bottom: 0.3em; }

figure {
  margin: 5mm 0 6mm;
  break-inside: avoid;
  text-align: center;
}

figure img {
  width: 100%;
  max-width: 100%;
  height: auto;
}

figcaption {
  margin-top: 2mm;
  font-size: 8.7pt;
  line-height: 1.4;
  color: #334e68;
  text-align: left;
}

.fig-label {
  font-family: "Liberation Sans", sans-serif;
  font-weight: bold;
  color: #0b2749;
}

.table-caption, .listing-caption {
  font-family: "Liberation Sans", sans-serif;
  font-size: 8.8pt;
  line-height: 1.4;
  color: #334e68;
  margin: 5mm 0 1.6mm;
  text-align: left;
  break-after: avoid;
}

.table-caption strong, .listing-caption strong { color: #0b2749; }

table {
  width: 100%;
  border-collapse: collapse;
  margin: 0 0 5mm;
  font-family: "Liberation Sans", sans-serif;
  font-size: 8.15pt;
  line-height: 1.34;
}

thead { display: table-header-group; }

th {
  background: #eaf1f9;
  color: #0b2749;
  text-align: left;
  font-weight: bold;
  padding: 1.7mm 2mm;
  border-top: 1pt solid #123a6b;
  border-bottom: 0.7pt solid #123a6b;
  vertical-align: bottom;
}

td {
  padding: 1.55mm 2mm;
  border-bottom: 0.4pt solid #d9e2ec;
  vertical-align: top;
}

tbody tr:nth-child(even) td { background: #f8fafc; }

tr { break-inside: avoid; }

pre {
  margin: 0 0 5mm;
  padding: 3.2mm 4mm;
  background: #f6f9fc;
  border-left: 1.6mm solid #0f766e;
  border-radius: 0 2mm 2mm 0;
  font-family: "Liberation Mono", "DejaVu Sans Mono", monospace;
  font-size: 6.9pt;
  line-height: 1.4;
  white-space: pre-wrap;
  overflow-wrap: break-word;
  break-inside: avoid;
}

pre code {
  font-size: inherit;
  background: none;
  padding: 0;
}

blockquote {
  margin: 4mm 0;
  padding: 2mm 4mm;
  border-left: 1.2mm solid #0f766e;
  color: #334e68;
}

/* ---------- references ---------- */

.references p {
  hyphens: manual;
  overflow-wrap: break-word;
  font-size: 8.8pt;
  line-height: 1.4;
  margin: 0 0 2.2mm;
  padding-left: 7mm;
  text-indent: -7mm;
  text-align: left;
}

/* ---------- print details ---------- */

h2, h3 { page-break-after: avoid; }

.appendix-note { font-size: 9pt; color: #52677d; }
"""


def main() -> None:
    meta, body_md = parse_source(SOURCE.read_text(encoding="utf-8"))

    md = markdown.Markdown(
        extensions=["extra", "toc", "sane_lists", "smarty"],
        extension_configs={
            "toc": {"permalink": False, "anchorlink": False, "toc_depth": "2-3"}
        },
    )
    body_html = md.convert(body_md)
    toc_html = build_toc(md.toc_tokens)

    # Bold the "Table 3.1." / "Listing 1." label inside captions.
    body_html = re.sub(
        r'(<p class="(?:table|listing)-caption"[^>]*>)(Table\s+[A-Z0-9.\-]+\.|Listing\s+\d+\.)',
        r"\1<strong>\2</strong>",
        body_html,
    )

    toc_section = f"""
<section class="toc-page">
  <h2 class="toc-title" id="contents">Contents</h2>
  {toc_html}
</section>
"""
    if "<!-- TOC -->" not in body_html:
        raise SystemExit("TOC marker not found in converted body")
    body_html = body_html.replace("<!-- TOC -->", toc_section)

    doc = f"""<!DOCTYPE html>
<html lang="en-GB">
<head>
<meta charset="utf-8">
<title>{html.escape(meta.get('title', 'Project Report'))} — {html.escape(meta.get('doctype', 'Report'))}</title>
<meta name="author" content="{html.escape(meta.get('authors', ''))}">
<meta name="description" content="{html.escape(meta.get('subtitle', ''))}">
<meta name="keywords" content="Jakarta EE, servlet, JSP, JDBC, MySQL, software engineering, web application">
<style>{CSS}</style>
</head>
<body>
{title_page(meta)}
{body_html}
</body>
</html>
"""
    HTML_OUT.write_text(doc, encoding="utf-8")
    HTML(string=doc, base_url=str(HERE)).write_pdf(str(PDF))
    print(f"wrote {PDF} ({PDF.stat().st_size // 1024} KiB)")
    print(f"wrote {HTML_OUT}")


if __name__ == "__main__":
    main()
