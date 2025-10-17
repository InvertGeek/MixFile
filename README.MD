


<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/InvertGeek/MixFile">
    <img src="https://invertgeek.github.io/mixfile-doc/logo.png" alt="Logo" width="200" height="200">
  </a>
  <h1 align="center">MixFile</h1>
  <p align="center">
    无限空间,不限速,加密储存
  </p>
</div>

## MixFile 介绍
MixFile是一款可以上传加密文件并分享的安卓APP \
可实现本地加密上传,加密下载文件,在线播放加密视频 \
加密时会自动生成随机密钥进行加密,上传完成后,密钥 文件地址 文件大小等信息会附加在分享码中 \
将分享码发送给他人即可实现分享文件 \
已发布版本: https://github.com/InvertGeek/MixFile/releases \
使用帮助: https://invertgeek.github.io/mixfile-doc/help.html \
TG群: https://t.me/ivgeek

### 功能
支持加密上传下载，批量分享文件 \
支持WebDav协议,使用Alist,Raildrive等软件挂载为硬盘 \
支持加密视频在线播放，支持切换进度条,直接获取加密文件流

### 安全性:
使用AES-256算法加密，绝对安全，即使是量子计算机也无法破解 \
文件是在本地加密后进行上传，除了你本人，即使是服务端也不知道文件内容 

### 防篡改:
每个文件分片都会使用sha256哈希校验,即使是使用相同算法相同秘钥二次加密不同内容也可检测出 \
检测到篡改立刻中断流量,即使是视频播放，切换到中间进度，任何一个字节被修改都会导致播放中断  \
保证同一个分享码，不可能下载到不同的文件,只要分享码内容相同,无论以什么手段修改云端的返回的内容都是无法通过校验的

### 可拓展:
支持js自定义线路,编写脚本即可将任何图床站点变为储存服务端

### 相关项目:
mixfile-core(MixFile核心模块实现): https://github.com/InvertGeek/mixfile-core 

mixfile-scripts(js自定义线路脚本): https://github.com/InvertGeek/mixfile-scripts 

mixfile-cli(mixfile命令行端): https://github.com/InvertGeek/mixfilecli

mixfile-front(mixfile网页端源码): https://github.com/InvertGeek/mixfilefront

MixMessage(在QQ微信等软件使用加密聊天,发送文件由mixfile-core实现): https://github.com/InvertGeek/MixMessage

mixfile-http线路例子(现在推荐使用js自定义线路): https://github.com/InvertGeek/mixfileexamplejs

mixfile-alist(使用Alist作为线路(http)): https://github.com/muxinxy/mixfile-alist

### 注意事项
安卓端默认使用的是短分享码，其中填充了大量不可见的字符 \
在tg github中发出会被过滤，所以issue反馈问题请使用长分享码 \
测试qq 微信等软件不会屏蔽 \
WebDAV数据和收藏的文件是分开储存的，格式不同 \
如果要在WebDAV中导入收藏数据 \
导出收藏文件然后通过WebDAV安卓端界面或上传mix_list文件到WebDAV中导入即可



## 免责声明

+   不能百分百保证文件永久有效,请自行对重要文件做好本地备份。
+   请勿使用本项目上传不符合社会主义核心价值观的文件。
+   该项目仅用于学习和技术交流，开发者不承担任何由使用者的行为带来的法律责任。

## Github Star
[![Stargazers over time](https://starchart.cc/InvertGeek/MixFile.svg?variant=adaptive)](https://starchart.cc/InvertGeek/MixFile)