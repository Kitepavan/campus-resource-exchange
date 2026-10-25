-- Migration 001 — 2026-10-25
-- Adds the DAILY_ESSENTIAL resource type introduced upstream.
-- A fresh database/schema.sql already contains this; existing databases need:
--
--   mysql -u campus_app -p campus_exchange < database/migrations/001_add_daily_essential.sql

ALTER TABLE resources
    MODIFY resource_type ENUM('BOOK', 'LAB_EQUIPMENT', 'STUDY_MATERIAL', 'DAILY_ESSENTIAL')
    NOT NULL;
