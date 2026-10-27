function isNull(value) {
    return value == null || String(value).trim() === '';
}

/** 最大长度包含边界；正文只校验必填与长度，允许正常标点和换行。 */
function validLength(value, limit) {
    return value != null && String(value).trim().length <= limit;
}

function validCommentBody(value) {
    return !isNull(value) && validLength(value, 200);
}

// 与数据库 commentator varchar(50) 保持一致
function validCN_ENString2_50(value) {
    return value != null && /^[a-zA-Z0-9-\u4E00-\u9FA5_,， ]{2,50}$/.test(String(value).trim());
}
