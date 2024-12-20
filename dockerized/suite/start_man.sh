docker-compose down
docker build -t mapp ../mapp/webapp/
docker build -t msrv ../msrv
docker-compose up

