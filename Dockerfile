FROM gradle:jdk20

WORKDIR /app

ADD . .

RUN gradle distTar

WORKDIR /app/build/distributions

RUN tar -xf GlobalBan-1.0.0.tar

CMD ./GlobalBan-1.0.0/bin/GlobalBan