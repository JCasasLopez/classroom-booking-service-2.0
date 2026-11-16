CREATE TABLE IF NOT EXISTS bookings (
    idBooking BIGINT NOT NULL AUTO_INCREMENT,
    idUser INT NOT NULL,
    idClassroom INT NOT NULL,
    start DATETIME NOT NULL,
    finish DATETIME NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    status ENUM('ACTIVE', 'CANCELLED', 'COMPLETED') NOT NULL,
    PRIMARY KEY (idBooking)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS watch_alerts (
    idWatchAlert BIGINT NOT NULL AUTO_INCREMENT,
    idBooking BIGINT NOT NULL,
    userEmail VARCHAR(255) NOT NULL,
    PRIMARY KEY (idWatchAlert)
) ENGINE=InnoDB;

CREATE TABLE classroom_locks (
    id_classroom INT NOT NULL,
    PRIMARY KEY (id_classroom)
) ENGINE=InnoDB;

CREATE TABLE notification_outbox (
    id_notification BIGINT NOT NULL AUTO_INCREMENT,
    subject VARCHAR(255),
    message TEXT,
    email_address VARCHAR(255),
    timestamp DATETIME,
    PRIMARY KEY (id_notification)
) ENGINE=InnoDB;