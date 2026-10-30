-- Iba pre staru tabulku s full_time BOOLEAN/TINYINT/BIT. Spustite jednorazovo.
-- Pred spustenim exportujte zalohu databazy v phpMyAdmin.
USE employee;
ALTER TABLE employee ADD COLUMN full_time_text VARCHAR(30) NULL;
UPDATE employee
SET full_time_text = CASE
    WHEN full_time = 1 THEN 'Full-time'
    WHEN full_time = 0 THEN 'Part-time'
    ELSE NULL
END;
ALTER TABLE employee DROP COLUMN full_time;
ALTER TABLE employee CHANGE COLUMN full_time_text full_time VARCHAR(30) NOT NULL;
-- Povodne nazvy job_title zjednotte s job-titles v application.properties,
-- alebo pridajte svoje existujuce pozicie do tohto zoznamu.
