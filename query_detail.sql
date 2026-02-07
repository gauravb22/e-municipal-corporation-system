SET PAGESIZE 100;
SET LINESIZE 150;
SET ECHO OFF;

PROMPT ==================== USERS TABLE STRUCTURE ====================
DESC users;

PROMPT 
PROMPT ==================== TEST TABLE STRUCTURE ====================
DESC test;

PROMPT 
PROMPT ==================== ALL COLUMNS IN USERS TABLE ====================
SELECT column_name, data_type, nullable, data_length FROM user_tab_columns WHERE table_name='USERS' ORDER BY column_id;

PROMPT 
PROMPT ==================== DATA IN USERS TABLE ====================
SELECT * FROM users;

PROMPT 
PROMPT ==================== DATA IN TEST TABLE ====================
SELECT * FROM test;

EXIT;
