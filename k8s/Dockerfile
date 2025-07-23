FROM php:8.1-apache
RUN apt-get update && docker-php-ext-install mysqli pdo && docker-php-ext-enable mysqli pdo
COPY app/index.php /var/www/html/
RUN chmod 755 /var/www/html/index.php
