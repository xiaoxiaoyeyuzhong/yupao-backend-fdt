-- auto-generated definition
# 新建用户表
create table user
(
    id           bigint auto_increment comment 'id'
        primary key,
    username     varchar(256)                       null comment '用户昵称',
    userAccount  varchar(256)                       null comment '账号',
    avatarUrl    varchar(1024)                      null comment '用户头像',
    gender       tinyint                            null comment '性别',
    userPassword varchar(512)                       not null comment '密码',
    phone        varchar(128)                       null comment '电话',
    email        varchar(512)                       null comment '邮箱',
    status       int      default 0                 not null comment '用户状态 0-正常',
    createTime   datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP null comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    userRole     int      default 0                 not null comment '用户角色 0- 普通用户 1- 管理员 ',
    planetCode   varchar(512)                       null comment '星球编号'
)
    comment '用户';

# 修改用户表
alter table user
    add COLUMN tags varchar(1024) null comment '标签列表';

alter table user
    add profile varchar(512) null comment '个人简介';

#新建标签表
create table tag
(
    id         bigint auto_increment comment 'id'
        primary key,
    tagName    varchar(256)                       null comment '标签名称',
    userId     bigint                             null comment '上传标签的用户id',
    parentId   bigint                             null comment '父标签id',
    isParent   tinyint                            null comment '是否为父标签 0-否 1-是',
    createTime datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP null comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除 0-不删除 1-删除'
)
    comment '标签';

# 新建队伍表
create table if not exists team
(
    id          bigint auto_increment comment 'id' primary key,
    name        varchar(256)                       not null comment '队伍名称',
    description varchar(1024)                      null comment '描述',
    maxNum      int      default 1                 not null comment '最大人数',
    expireTime  datetime                           null comment '过期时间',
    userId      bigint                             not null comment '创建人id',
    status      int      default 0                 not null comment '0-公开，1-私有 2-加密',
    password    varchar(512)                       null comment '密码',
    createTime  datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP null comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除'
) comment '队伍';

#新建用队伍关系表
create table if not exists team_user
(
    id         bigint auto_increment comment 'id' primary key,
    userId     bigint                             comment '用户id',
    teamId     bigint                             comment '队伍id',
    joinTime   datetime                           null comment '加入时间',
    createTime datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除'
) comment '用户队伍关系';