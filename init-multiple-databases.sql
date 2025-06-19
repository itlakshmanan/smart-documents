CREATE DATABASE IF NOT EXISTS books;
CREATE DATABASE IF NOT EXISTS orders;

CREATE USER IF NOT EXISTS 'book_user'@'%' IDENTIFIED BY 'book_password';
CREATE USER IF NOT EXISTS 'order_user'@'%' IDENTIFIED BY 'order_password';

GRANT ALL PRIVILEGES ON books.* TO 'book_user'@'%';
GRANT ALL PRIVILEGES ON orders.* TO 'order_user'@'%';

FLUSH PRIVILEGES;
