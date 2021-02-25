package com.rookiefly.open.dubbo.dayu.model.vo;

import lombok.Data;

@Data
public class ResultVO {

    private boolean success;
    private String code = "000";
    private String msg = "";
    private Object data;

    public static ResultVO wrapSuccessfulResult() {
        ResultVO vo = new ResultVO();
        vo.setSuccess(true);
        return vo;
    }

    public static ResultVO wrapSuccessfulResult(Object data) {
        ResultVO vo = new ResultVO();
        vo.setSuccess(true);
        vo.setData(data);
        return vo;
    }

    public static ResultVO wrapErrorResult(String msgStr) {
        ResultVO vo = new ResultVO();
        vo.setSuccess(false);

        vo.setMsg(msgStr);

        return vo;
    }

    public static ResultVO wrapErrorResult(String code, String msgStr) {
        ResultVO vo = new ResultVO();
        vo.setCode(code);
        vo.setSuccess(false);
        vo.setMsg(msgStr);

        return vo;
    }
}
