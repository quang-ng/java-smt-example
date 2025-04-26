PROJECT_NAME=java-smt-example
CONNECTOR_NAME=mysql-employee-connector
CONNECT_URL=http://localhost:8083/connectors

.PHONY: all build up down restart logs register unregister clean mysql

all: build up register

build:
	docker-compose build

up:
	docker-compose up

run:
	docker-compose up --build

down:
	docker-compose down

restart: down build up

logs:
	docker-compose logs -f

register:
	curl -X POST $(CONNECT_URL) \
		-H "Content-Type: application/json" \
		-d @register-connector.json

unregister:
	curl -X DELETE $(CONNECT_URL)/$(CONNECTOR_NAME)

register-status:
	curl -X GET $(CONNECT_URL)/$(CONNECTOR_NAME)/status | jq '.'

clean: down
	docker volume rm $$(docker volume ls -qf dangling=true)
	docker system prune -f

runmysql:
	docker exec -it $$(docker-compose ps -q mysql) mysql -uroot -proot demo
