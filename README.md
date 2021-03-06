# Vision

## 介绍
Vision是一款集成多设备优势的跨手机、手表、云台的智能图像控制、采集、融合方案。


### 实现功能

（1）多设备互联：多设备在同一局域网下进行连接，可选择作为采集端或控制端，我们考虑的多设备包含控制端手机、采集端手机、手表、云台等。

（2）跨设备操作：采集端负责提供摄像头画面给控制端，同时也可以控制自己的设备；控制端不仅可以查看采集端画面，也能对采集端进行参数调整、拍摄录制等操作；

（3）一键同时操作多设备：控制端可以一键操控多设备开始同时拍摄或录制，来对一个场景不同角度的画面进行捕捉。

（4）同屏实时显示多设备画面：用户可以选择查看一个或多个想要观察的画面；


### 实现场景

（1）合影场景：设计了远程预览和远程控制拍摄，便于调整后进行合影，可以应用到手表端。

（2）多角度捕捉：可用于多设备对一个拍摄对象进行多角度拍摄、对运动时某一个瞬间的动作情况进行捕捉、得到一个场景的全视角融合结果

（3）多机位录制：可用做举办小型活动时的小型导播台，实现实时查看由各终端组成的多机位情况、随时切换多设备的角度记录活动现场画面，可以使用集成人脸识别模块的云台操作。


### 创新点

（1）目前已有方案均采用以云服务器为主要数据传输载体，本项目实时和无损数据传输均主要通过端到端进行，大幅度减轻云服务器压力

（2）本项目利用多设备采集优势，实现远程合拍、多角度捕捉、多机位录制，并实现相关高清晰度的一体融合效果

（3）本项目实现对多设备相机资源的实时预览、控制，弥补市场已有抓拍方案的单一性、固定性，同时采集端仍然有自主控制权

（4）智能化手机端人脸识别控制云台跟随

### 屏幕快照
<div align=center>
<img src="Screenshots1.png" height="600"  />
<img src="Screenshots2.png" height="600" />
<img src="Screenshots4.png" height="600"  />
</div>

<div align=center>
<img src="Screenshots3.png" width="200"  />
<img src="Screenshots5.png" width="200"/>
</div>

## 软件架构
### 分支介绍

1. master分支为主分支，包含最新版代码
2. watch分支为适配于手表端的分支
3. 其他分支为开发中间过程分支

### Android源码架构

```markdown
├─arm_controller 云台控制模块
│  ├─commen
│  ├─connect
│  ├─model
│  ├─uitls
│  └─widget
├─bean 元数据类型
├─entrance 启动预览画面
├─transfer  端到端文件传输模块
│  ├─adapter
│  ├─app
│  ├─broadcast
│  ├─callback
│  ├─client
│  ├─common
│  ├─map
│  ├─model
│  ├─server
│  ├─util
│  ├─utils
│  └─widget
├─ui UI模块及代码
├─ui_utils UI模块的小功能
├─utils 各类辅助方法
├─view 辅助View
├─webRTC_utils webRTC改进实现预览
└─ws WebSocket使用集成
```

## 模块详细设计

详见[详细设计文档](doc/Vision详细设计文档.docx)

## 安装教程

1.  Android Studio导入项目
2.  gradle build
3.  配置云台等环境（如果需要的话）
4.  run app on devices

## 使用说明

详见[项目使用手册](doc/Vision项目使用手册.docx)


## 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request

