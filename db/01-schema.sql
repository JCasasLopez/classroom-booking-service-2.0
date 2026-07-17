CREATE TABLE IF NOT EXISTS bookings (
    id_booking BIGINT NOT NULL AUTO_INCREMENT,
    id_user INT NOT NULL,
    id_classroom INT NOT NULL,
    start DATETIME NOT NULL,
    finish DATETIME NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    status ENUM('ACTIVE', 'CANCELLED', 'COMPLETED') NOT NULL,
    PRIMARY KEY (id_booking)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS watch_alerts (
    id_watch_alert BIGINT NOT NULL AUTO_INCREMENT,
    id_booking BIGINT NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    PRIMARY KEY (id_watch_alert)
) ENGINE=InnoDB;