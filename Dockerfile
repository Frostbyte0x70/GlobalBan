FROM gradle:jdk19

WORKDIR /app

ADD . .

RUN gradle distTar

WORKDIR /app/build/distributions

RUN tar -xf globalban-1.0.0.tar

CMD ./globalban-1.0.0/bin/globalban