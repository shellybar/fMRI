# noinspection SqlNoDataSourceInspectionForFile

# drop
DROP TRIGGER IF EXISTS before_update_tasks;
DROP TABLE IF EXISTS tasks;
DROP TABLE IF EXISTS flows;
DROP TABLE IF EXISTS units;

# units table
CREATE TABLE units (
  analysis_unit_id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  analysis_name VARCHAR(50) NOT NULL,
  serial INT UNSIGNED NOT NULL,
  external_unit_id INT UNSIGNED NOT NULL, # unitId from XML configuration file
  insertion_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (analysis_unit_id),
  UNIQUE INDEX unit_serial_UNIQUE (analysis_name ASC, serial ASC),
  UNIQUE INDEX analysis_unit_id_UNIQUE (analysis_unit_id ASC))
  ENGINE = InnoDB;

# flows table
CREATE TABLE flows (
  flow_id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  study_name VARCHAR(50) NOT NULL,
  insertion_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status VARCHAR(20) NOT NULL DEFAULT 'New',
  PRIMARY KEY (flow_id),
  UNIQUE INDEX flow_id_UNIQUE (flow_id ASC))
  ENGINE = InnoDB;

# tasks table
CREATE TABLE tasks (
  task_id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  status VARCHAR(20) NOT NULL,
  flow_id INT UNSIGNED NOT NULL,
  serial_in_flow INT UNSIGNED NOT NULL,
  unit_id INT UNSIGNED NOT NULL,
  unit_params BLOB NULL,
  subject VARCHAR(50) NULL,
  run VARCHAR(50) NULL,
  machine_id INT UNSIGNED NULL,
  insertion_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  execution_time TIMESTAMP NULL,
  completion_time TIMESTAMP NULL,
  last_updated TIMESTAMP NULL,
  PRIMARY KEY (task_id),
  UNIQUE INDEX task_id_UNIQUE (task_id ASC),
  INDEX fk_flow_id_idx (flow_id ASC),
  CONSTRAINT fk_flow_id
    FOREIGN KEY (flow_id)
    REFERENCES flows (flow_id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE)
  ENGINE = InnoDB;

# triggers on tasks table
DELIMITER $$
CREATE TRIGGER before_update_tasks BEFORE UPDATE ON tasks
FOR EACH ROW BEGIN SET
NEW.execution_time = (CASE WHEN NEW.status = 'Processing'
  THEN NOW() ELSE OLD.execution_time END),
NEW.completion_time = (CASE WHEN NEW.status = 'Completed'
  THEN NOW() ELSE OLD.completion_time END),
NEW.last_updated = NOW();
END
$$ DELIMITER ;

######################################### DEBUG TABLES #####################################

# drop
DROP TRIGGER IF EXISTS zz_debug_before_update_tasks;
DROP TABLE IF EXISTS zz_debug_tasks;
DROP TABLE IF EXISTS zz_debug_flows;
DROP TABLE IF EXISTS zz_debug_units;

# units table
CREATE TABLE zz_debug_units (
  analysis_unit_id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  analysis_name VARCHAR(50) NOT NULL,
  serial INT UNSIGNED NOT NULL,
  external_unit_id INT UNSIGNED NOT NULL, # unitId from XML configuration file
  insertion_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (analysis_unit_id),
  UNIQUE INDEX zz_debug_unit_serial_UNIQUE (analysis_name ASC, serial ASC),
  UNIQUE INDEX zz_debug_analysis_unit_id_UNIQUE (analysis_unit_id ASC))
  ENGINE = InnoDB;

# flows table
CREATE TABLE zz_debug_flows (
  flow_id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  study_name VARCHAR(50) NOT NULL,
  insertion_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status VARCHAR(20) NOT NULL DEFAULT 'New',
  PRIMARY KEY (flow_id),
  UNIQUE INDEX zz_debug_flow_id_UNIQUE (flow_id ASC))
  ENGINE = InnoDB;

# tasks table
CREATE TABLE zz_debug_tasks (
  task_id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  status VARCHAR(20) NOT NULL,
  flow_id INT UNSIGNED NOT NULL,
  serial_in_flow INT UNSIGNED NOT NULL,
  unit_id INT UNSIGNED NOT NULL,
  unit_params BLOB NULL,
  subject VARCHAR(50) NULL,
  run VARCHAR(50) NULL,
  machine_id INT UNSIGNED NULL,
  insertion_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  execution_time TIMESTAMP NULL,
  completion_time TIMESTAMP NULL,
  last_updated TIMESTAMP NULL,
  PRIMARY KEY (task_id),
  UNIQUE INDEX zz_debug_task_id_UNIQUE (task_id ASC),
  INDEX zz_debug_fk_flow_id_idx (flow_id ASC),
  CONSTRAINT zz_debug_fk_flow_id
  FOREIGN KEY (flow_id)
  REFERENCES zz_debug_flows (flow_id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE)
  ENGINE = InnoDB;

# triggers on tasks table
DELIMITER $$
CREATE TRIGGER zz_debug_before_update_tasks BEFORE UPDATE ON zz_debug_tasks
FOR EACH ROW BEGIN SET
NEW.execution_time = (CASE WHEN NEW.status = 'Processing'
  THEN NOW() ELSE OLD.execution_time END),
NEW.completion_time = (CASE WHEN NEW.status = 'Completed'
  THEN NOW() ELSE OLD.completion_time END),
NEW.last_updated = NOW();
END
$$ DELIMITER ;