<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/InvertGeek/MixFile">
    <img src="https://invertgeek.github.io/mixfile-doc/logo.png" alt="Logo" width="200" height="200">
  </a>

  <h1 align="center">MixFile</h1>

  <p align="center">
    无限空间 · 不限速 · 加密储存  
    <br />
    <a href="https://github.com/InvertGeek/MixFile/releases">📦 已发布版本</a> •
    <a href="https://invertgeek.github.io/mixfile-doc/help.html">📘 使用帮助</a> •
    <a href="https://t.me/ivgeek">💬 TG 群</a>
  </p>
</div>

---

## 📖 MixFile 介绍

**MixFile** 是一款可以上传加密文件并分享的安卓 APP。  
支持本地加密上传、加密下载文件、在线播放加密视频。  

加密时会自动生成随机密钥进行加密，上传完成后，密钥、文件地址、文件大小等信息会附加在 **分享码** 中。  
只需将分享码发送给他人，即可安全分享文件。

---

## ⚙️ 功能特性

- 🔐 **加密上传 / 下载**，支持批量分享文件  
- 💾 **WebDav 支持**：可配合 Alist、RailDrive 等软件挂载为硬盘  
- 🎬 **加密视频播放**：支持进度条切换，直接播放加密文件流  

---

## 🛡️ 安全性

- 使用 **AES-256** 算法加密，安全级别极高  
- 文件在本地加密后上传，**服务端无法得知文件内容**

---

## 🧱 防篡改机制

- 每个文件分片均使用 **SHA256 哈希校验**  
- 即使相同算法与密钥二次加密不同内容，也可检测篡改  
- 一旦检测到修改，立即中断流量传输（包括视频播放）  
- 保证同一分享码下的文件内容**完全一致、不可伪造**

---

## 🧩 可拓展性

支持 **JavaScript 自定义线路**，  
编写脚本即可将任意图床或存储服务变为 MixFile 储存节点。

---

## 🧱 相关项目

| 模块 | 描述 | 仓库地址 |
|------|------|----------|
| **mixfile-core** | 核心加密与逻辑实现 | [GitHub](https://github.com/InvertGeek/mixfile-core) |
| **mixfile-scripts** | JS 自定义线路脚本 | [GitHub](https://github.com/InvertGeek/mixfile-scripts) |
| **mixfile-cli** | 命令行端工具 | [GitHub](https://github.com/InvertGeek/mixfilecli) |
| **mixfile-front** | 网页端源码 | [GitHub](https://github.com/InvertGeek/mixfilefront) |
| **MixMessage** | QQ/微信等平台加密聊天 | [GitHub](https://github.com/InvertGeek/MixMessage) |
| **MixGram** | 基于 Git 的加密聊天 | [GitHub](https://github.com/InvertGeek/MixGram) |
| **mixfileexamplejs** | HTTP 线路示例（旧版） | [GitHub](https://github.com/InvertGeek/mixfileexamplejs) |
| **mixfile-alist** | 基于 Alist 的 HTTP 线路 | [GitHub](https://github.com/muxinxy/mixfile-alist) |

---

## ⚠️ 注意事项

- 安卓端默认使用**短分享码**，其中包含大量不可见字符  
  - 在 Telegram / GitHub 发送时可能被过滤  
  - **反馈问题请使用长分享码**
- QQ / 微信等平台测试分享码不会被屏蔽  
- WebDAV 数据与收藏文件分开存储，格式不同  
  - 若要在 WebDAV 导入收藏：  
    导出收藏文件后，通过 WebDAV 安卓端界面上传 `mix_list` 文件导入即可

---

## 🧾 免责声明

- ⚠️ 不能百分百保证文件永久有效，请自行备份重要文件。  
- 🚫 请勿上传违反社会主义核心价值观的内容。  
- 📚 本项目仅用于学习与技术交流，开发者不对使用者行为承担法律责任。

---

## ⭐ Github Star

[![Stargazers over time](https://starchart.cc/InvertGeek/MixFile.svg?variant=adaptive)](https://starchart.cc/InvertGeek/MixFile)
