DROP TABLE IF EXISTS codes;
DROP TABLE IF EXISTS crypto_datas;
CREATE TABLE 
IF NOT EXISTS
codes
(
	id INT PRIMARY KEY AUTO_INCREMENT NOT NULL, 
	code VARCHAR(5) NOT NULL
);
CREATE TABLE 
IF NOT EXISTS
crypto_datas
(
	id INT PRIMARY KEY AUTO_INCREMENT NOT NULL,
	code VARCHAR(5) NOT NULL,
	start_time TIMESTAMP NOT NULL,
	end_time TIMESTAMP NOT NULL,
	oldest DOUBLE PRECISION NOT NULL,
	newest DOUBLE PRECISION NOT NULL,
	min DOUBLE PRECISION NOT NULL,
	max DOUBLE PRECISION NOT NULL,
	normalized_range DOUBLE PRECISION NOT NULL
);

INSERT INTO codes (code) VALUES ('BTC');
INSERT INTO codes (code) VALUES ('DOGE');
INSERT INTO codes (code) VALUES ('ETH');
INSERT INTO codes (code) VALUES ('LTC');
INSERT INTO codes (code) VALUES ('XRP');