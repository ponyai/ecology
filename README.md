# ecology
ecology is the project for OA development
本地搭建oa开发环境
下载ecology/classbean 下class文件作为本地项目source
下载WEB-INF/lib 下jar包作为本地项目source

定时任务
1.java类继承weaver.interfaces.schedule.BaseCronJob类，重写execute()方法。 （execute()方法无参）
2.计划任务类填写java类的全路径。
3.如果需要添加参数，需要在java类中添加private 变量，并添加set get方法。
定时任务类放到 ecology/classbean
/usr/weaver/ecology/classbean/weaver/interfaces/schedule

文件变更需重启
重启服务
/usr/weaver/Resin4/bin
sh stopresin.sh
sh startresin.sh
