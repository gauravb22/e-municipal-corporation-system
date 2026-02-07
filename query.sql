SET PAGESIZE 50;
SET LINESIZE 100;
SET ECHO OFF;
PROMPT ==================== TABLES IN YOUR DATABASE ====================
SELECT table_name FROM user_tables ORDER BY table_name;
PROMPT 
PROMPT ==================== TOTAL TABLE COUNT ====================
SELECT COUNT(*) as TOTAL_TABLES FROM user_tables;
PROMPT 
PROMPT ==================== TABLE DETAILS ====================
SELECT table_name, num_rows, blocks FROM user_tables ORDER BY table_name;
EXIT;
