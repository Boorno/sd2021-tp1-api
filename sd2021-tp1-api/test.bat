CALL mvn clean compile assembly:single docker:build
docker kill container1
docker rm container1
docker pull nunopreguica/sd2021-tester-tp1
docker run --rm --network=sdnet --name container1 -it -v /var/run/docker.sock:/var/run/docker.sock nunopreguica/sd2021-tester-tp1:latest -image sd2021-tp1-55397-55836 -sleep 1 -test 2