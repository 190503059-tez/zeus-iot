{
"jsonrpc": "2.0",
"method": "valuemap.update",
"params": {
"name": "${valueMapName}",
"valuemapid":${valueMapId},
"mappings": [
<#if valMaps??>
    <#list valMaps?keys as key>
        {
        "type": "0", <#--ȫ����ȷƥ��-->
        "value": "${key}",
        "newvalue": "${valMaps[key]}"
        }<#if key_has_next>,</#if>
    </#list>
</#if>
]
},
"auth": "${userAuth}",
"id": 1
}