{
    "jsonrpc": "2.0",
    "method": "history.get",
    "params": {
        "output": "extend",
        "history": ${valueType},
        <#if hostid??>
            "hostids": "${hostid}",
        </#if>
        <#if itemids??>
            "itemids": [
            <#list itemids as itemid>
                ${itemid}<#if itemid_has_next>,</#if>
            </#list>
            ],
        </#if>
        "sortfield": "clock",
        "sortorder": "DESC",
        "limit": ${hisNum}
    },
    "auth": "${userAuth}",
    "id": 1
}