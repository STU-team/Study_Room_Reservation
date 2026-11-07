package com.studyroom.dao;

/** DAO 层文件读写或数据格式异常；与业务校验异常 BusinessException 区分。 */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}
