/**
 * Created by lev on 09.03.2017.
 */
Ext.define('BillWebApp.store.OrgCurUserStore', {
    extend: 'Ext.data.Store',
    alias  : 'store.orgcuruserstore',
    storeId: 'OrgCurUserStore',
    model: 'BillWebApp.model.Org',
    config:{
        autoLoad: false,
        autoSync: false
    },
    proxy: { // используется совместно с параметрами roleCd, actCd
        autoSave: false,
        type: 'ajax',
        api: {
            create  : '',
            read    : 'http://192.168.100.21:8083/sec/getOrgCurUser',
            update  : '',
            destroy : ''
        },
        reader: {
            type: 'json'
        },
        writer: {
            type: 'json',
            allowSingle: false //запретить по одному отправлять отправлять объекты в Json - только массивом![объект] - иначе трудно описывать в Restful
            ,
            writeAllFields: true  //писать весь объект в json - не имеет смысла
        }
    }
});