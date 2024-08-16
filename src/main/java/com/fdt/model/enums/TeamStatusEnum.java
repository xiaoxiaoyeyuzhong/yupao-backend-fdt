package com.fdt.model.enums;

/**
 * 队伍状态枚举
 */
public enum TeamStatusEnum {

    PUBLIC(0,"公开"),
    PRIVATE(1,"私有"),
    SECRET(2,"加密");

    private int value;

    private String text;

    public static TeamStatusEnum getEnumByValue(Integer value) {
      if(value == null){
          return null;
      }
      TeamStatusEnum[] values = values();
      for (TeamStatusEnum teamStatusEnum : values) {
          if(teamStatusEnum.getValue() == value){
              return teamStatusEnum;
          }
      }
      return null;
    }

    TeamStatusEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public int getValue() {
        return value;
    }

    public TeamStatusEnum setValue(int value) {
        this.value = value;
        return this;
    }

    public String getText() {
        return text;
    }

    public TeamStatusEnum setText(String text) {
        this.text = text;
        return this;
    }
}
