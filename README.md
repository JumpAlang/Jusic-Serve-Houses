> 热烈庆祝一起听歌吧微信小程序上架成功，搜索：***灵魂自习室***

> 使用docker一键部署一起听歌吧应用，从此你也拥有了自己的点歌台，docker部署详见下方

> 一起听歌吧官方所使用服务器配置,趁双11新用户1年只要84元（建议买3年）：[阿里云ecs_t5_突发型](https://www.aliyun.com/minisite/goods?userCode=ze4tzlf9&share_source=copy_link)
> 趁双11腾讯云新用户1年只要88元（建设买3年）：[腾讯云标准型S4](https://cloud.tencent.com/act/cps/redirect?redirect=10140&cps_key=52c40793a9f078023fbc4d27eee65032&from=activity)

> 目前服务器配置比较弱鸡，经常炸机，欢迎大佬赞助。mail to me .

> 也欢迎小伙伴提交自己搭建的地址到issue,如果可以，创办个一起听歌吧联盟，把所有一起听歌吧的地址聚合在一起。分解服务器压力。


如果遇到问题可以在本项目提 issue


## 项目背景

此版本是多房间版，在jusic-serve的基础上[Jusic-serve](https://github.com/JumpAlang/Jusic-serve)

后端: 本项目

前端: [Jusic-ui](https://github.com/JumpAlang/Jusic-ui/tree/jusic-ui-houses)
小程序: [Jusic-mini](https://github.com/JumpAlang/Jusic_mini)



## docker部署
> 一起听歌吧官方所使用服务器配置,新用户有优惠价100左右：[阿里云ecs_t5_突发型](https://www.aliyun.com/minisite/goods?userCode=ze4tzlf9&share_source=copy_link)
>
1.只要nodejs音乐api接口
  请使用<https://hub.docker.com/r/jumpalang/jusic_music_api>查看说明
   
2.只要java后端服务
  请使用<https://hub.docker.com/r/jumpalang/jusic_serve_houses>查看说明
  
3.如果想整套部署

 3.1 使用根目录下的docker-compose.yml，修改参数后，执行`docker-compose up -d` 然后在浏览器访问：<http://localhost:8888>
 
  参数说明：
  * 接口认证用户名：APIUSER，默认admin
  * 接口认证密码：APIPWD 默认123456 
  * Redis Host:RedisHost 默认redis，如果不是docker启动的redis，在本地可以直接填写localhost
  * 音乐api host:MusicApi 默认http://jusicMusicApi，如果不是docker启动的音乐api，在本地可以填写http://localhost
  * 音乐链接过期时间：MusicExpireTime 默认1200000毫秒
  * 获取音乐失败重试次数：ReTryCount 默认1次
  * 投票切歌率：VoteRate 默认0.3 
  * 网易热歌榜歌单id：WyTopUrl 默认3778678
  * 个人Server酱接口：ServerJUrl 默认https://sc.ftqq.com/SCU64668T909ada7955daadfb64d5e7652b93fb135dad06e659369.send，必须修改，否则当有用户@管理员时，消息会发到我这里
  * 每个ip限制创建房间数：IpHouse 默认3个
  * 系统最多可创建房间数：HouseSize 默认32个
 
 3.2 使用Jonnyan404小伙伴制作的docker
> 感谢小伙伴制作的docker <https://github.com/Jonnyan404>


`docker run -d --name music -p 8888:8888 jonnyan404/jusic`


## 安装

1. 克隆项目

   ```
   git clone https://github.com/JumpAlang/Jusic-Serve-Houses.git
   ```

   

2. 安装 Redis

   [Redis](https://redis.io/)

3. 安装音乐基础服务

   3.1 网易云音乐：[NeteaseCloudMusicApi](https://github.com/Binaryify/NeteaseCloudMusicApi)
   
   3.2 qq音乐:<https://github.com/jsososo/QQMusicApi>
   
   3.3 咪咕音乐：<https://github.com/JumpAlang/MiguMusicApi>
   
   3.4 铜钟forJusic(引入了酷我与虾米，当网易或者qq或者虾米找不到资源时，根据歌手名+歌曲名从酷我和虾米搜索)：<https://github.com/JumpAlang/tongzhongForJusic>
4. 配置

   在 `src\main\resources\application.yml` 中配置好 redis 以及音乐服务

5. 打包项目

   ```
   # 项目是使用 maven 构建的，可以用下面的命令把项目打包成 jar 文件
   > mvn package
   # 如果觉得打包过程太久，那么可以选择下面这条命令跳过打包时的项目测试
   > mvn package -Dmaven.test.skip
   ```

   

6. 启动项目

   ```
   > java -jar jusic-serve.jar
   ```

   

7. 前端

   请前往[Jusic-ui](https://github.com/JumpAlang/Jusic-ui/tree/jusic-ui-houses)项目



## 使用

1. 普通用户
   - 如果你想要点首歌可以在聊天窗口发送 `点歌 关键字`
   - 如果你不想听当前播放的音乐，那么你可以发起投票切换音乐 `投票切歌`
   - 如果你想要给自己修改一个昵称，那么你可以在聊天窗口发送 `设置昵称 名字`
   - 删除音乐 `删除音乐 歌曲名`
2. 管理员
   - 如果你是点歌台的管理员，那么你可以这样获取网站的权限 `root password` 或者 `admin password`
   - 如果管理员觉得某一首音乐排序太靠后了，那么可以发送 `置顶音乐 音乐名`
   - 如果管理员觉得某一首音乐太难听，你可以移除播放列表 `删除音乐 音乐名`
   - 如果管理员发现有人在点歌台捣乱，那么你可以这样 `拉黑用户 用户id`
   - 如果管理员发现拉黑错了人，点歌台还提供另外一条指令 `漂白用户 用户id`
   - 如果管理员不想某一首音乐再次被点播，那么管理员可以这样 `拉黑音乐 音乐id`
   - 如果管理员想要从黑名单中移除某一首音乐，那么可以 `漂白音乐 音乐id`
   - 清空歌曲列表 `清空列表`
   - 进入点赞模式，歌曲播放顺序将按照点赞数优先播放 `点赞模式`
   - 退出点赞模式 `退出点赞模式`
   - 音乐单名单 `音乐黑名单`
   - 用户黑名单 `用户黑名单`
   - 禁止切歌 `禁止切歌`
   - 启用切歌 `启用切歌`
   - 禁止点歌 `禁止点歌`
   - 启用点歌 `启用点歌`



## 在线预览

Jusic：[Jusic 点歌台](http://music.alang.run)

## 打赏请我喝奶茶
[打赏](http://www.alang.run/sponsor)



## 相关项目

* JusicServe:[JusicServe](https://github.com/hanhuoer/Jusic-serve)
* Jusic-ui:[Jusic-ui](https://github.com/hanhuoer/Jusic-ui)
* 网易云音乐api:[NeteaseMusic](https://github.com/jsososo/NeteaseMusic)
* qq音乐api:[qqMusicApi](https://github.com/jsososo/QQMusicApi)
* 咪咕音乐api:[miguMusicApi](https://github.com/jsososo/MiguMusicApi)



