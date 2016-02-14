# Create debug tasks table
DROP TABLE IF EXISTS zz_debug_tasks;
CREATE TABLE zz_debug_tasks (
  task_id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  status VARCHAR(20) NOT NULL,
  unit_id INT UNSIGNED NULL,
  unit_params BLOB NULL,
  machine_id INT UNSIGNED NULL,
  insertion_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  execution_time TIMESTAMP NULL,
  completion_time TIMESTAMP NULL,
  PRIMARY KEY (task_id),
  UNIQUE INDEX task_id_UNIQUE (task_id ASC))
  ENGINE = InnoDB;

# Create trigger on tasks table
DELIMITER $$
CREATE TRIGGER before_update BEFORE UPDATE ON zz_debug_tasks
FOR EACH ROW BEGIN SET
NEW.execution_time = (CASE WHEN NEW.status = 'Processing'
  THEN NOW() ELSE OLD.execution_time END),
NEW.completion_time = (CASE WHEN NEW.status = 'Completed'
  THEN NOW() ELSE OLD.completion_time END);
END
$$ DELIMITER ;