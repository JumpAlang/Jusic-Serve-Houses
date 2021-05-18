FROM java:8
EXPOSE 8888
WORKDIR /app
ADD target/jusic-serve.jar ./jusic-serve.jar

ENV APIUSER=admin APIPWD=123456 RedisHost=redis MusicApi=http://jusicMusicApi
ENV MusicExpireTime=1200000 ReTryCount=1 VoteRate=0.3 WyTopUrl=3778678
ENV ServerJUrl=https://sc.ftqq.com/SCU64668T909ada7955daadfb64d5e7652b93fb135dad06e659369.send
ENV IpHouse=3 HouseSize=32
ENV MiniId=wx693312f83f255cf3 MiniSecrect=eccc0908c65a4491da6811b3cbe3e986
ENV RoleRootPassword=654321


ENTRYPOINT java -jar -DAPIUSER=$APIUSER -DAPIPWD=$APIPWD -DRedisHost="$RedisHost" -DMusicApi="$MusicApi" -DMusicExpireTime=$MusicExpireTime -DReTryCount=$ReTryCount -DVoteRate=$VoteRate -DWyTopUrl=$WyTopUrl -DServerJUrl="$ServerJUrl" -DIpHouse=$IpHouse -DHouseSize=$HouseSize -DMiniId=$MiniId -DMiniSecrect=$MiniSecrect -DRoleRootPassword=$RoleRootPassword ./jusic-serve.jar