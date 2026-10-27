-- 已有数据库升级脚本：密码改为 BCrypt 存储（长度 60），需要先把列加长。
-- 执行一次即可；旧的 MD5 密码仍然可以登录，登录成功后会自动改存为 BCrypt。
ALTER TABLE `tb_admin_user`
  MODIFY `login_password` varchar(100) NOT NULL COMMENT '管理员登陆密码（BCrypt）';
