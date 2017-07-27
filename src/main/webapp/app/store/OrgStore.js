/**
 * Created by lev on 09.03.2017.
 */
Ext.define('BillWebApp.store.OrgStore', {
    extend: 'Ext.data.Store',
    alias  : 'store.orgstore',
    storeId: 'OrgStore',
    model: 'BillWebApp.model.Org',
    config:{
        autoLoad: true,
        autoSync: true
    },
    proxy: {
        autoSave: false,
        type: 'ajax',
        api: {
            create  : '',
            read    : '/base/getOrgAll',
            update  : '',
            destroy : ''
        },
        reader: {
            type: 'json'
        }
    }, listeners: {
        beforeload: function() {
            Ext.create('Ext.window.MessageBox', {
                multiline: false,
            }).show({
                title: 'Внимание!',
                msg: 'Дождитесь появления сообщения о загрузке приложения',
                closable: false,
                buttons: Ext.Msg.OK
            });
        }
        ,load: function() {
            console.log("OrgStore loaded!");
            Ext.create('Ext.window.MessageBox', {
                multiline: false,
            }).show({
                title: 'Внимание!',
                msg: 'Приложение загружено',
                closable: false,
                buttons: Ext.Msg.OK
            });

        }
    }
});