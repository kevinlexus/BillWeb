Ext.define('BillWebApp.view.main.AskObjPanel', {

    viewModel: {
        type: 'main'
    },

    extend: 'Ext.form.Panel',
    xtype: 'form-hboxlayout',
    alias: 'widget.askobjpanel',
    width: 600,
    height: 300,
    minWidth: 300,
    minHeight: 220,
    layout: 'form',
    plain: true,
    resizable : true,
    reference: 'askObjPanel',
    controller: 'askobjpanelcontroller',

    dockedItems: [{
        xtype: 'toolbar',
        flex: 1,
        dock: 'bottom',
        ui: 'footer',
        layout: {
            pack: 'end',
            type: 'hbox'
        },
        items: ['->', {
            xtype: 'button',
            text: 'Выбрать',
            listeners: {
                click: 'onAskObjButtonSelPress'
            }
        }/*,
        {
            fieldLabel: 'Last Name',
            bind: '{formTp}'
        }*/]
    }],

    items: [{
        xtype: 'fieldset',
        defaultType: 'textfield',
        layout: 'anchor',
        items: [
            {   xtype: 'combo',
                typeAhead: true,
                forceSelection: true,
                fieldLabel: 'Тип',
                labelWidth: 100,
                displayField: 'name',
                valueField: 'id',
                triggerAction: 'all',
                reference: 'askObjComboAdrTp',
                bind: {
                    store: '{addrMainTpStore}'
                }
            },
            {
                xtype: 'fieldcontainer',
                layout: 'hbox',
                align: 'stretch',
                combineErrors: true,

                items: [{
                    xtype: 'textfield',
                    name: 'objName',
                    flex: 8,
                    emptyText: 'Введите наименование',
                    allowBlank: false,
                    reference: 'askObjName'
                }, {
                    flex: 2,
                    xtype: 'button',
                    text: 'Найти',
                    listeners: {
                        click: 'onAskObjButtonFindPress'
                    }
                }]
            },
            {
            xtype: 'container',
            layout: 'vbox',
            layout:'fit',
            defaultType: 'textfield',

            items: [

                {
                    // Объекты для выбора
                    xtype: 'gridpanel',
                    reference: 'askObjGrid',
                    //flex: 1,
//                    minHeight: 100,
                    height: 170,
                    hideHeaders: true,
                    bind: {
                        store: '{koAddrTpStore}'
                    },
                    columns: [
                        { text: 'Id',  dataIndex: 'id', width: 100
                        },
                        { text: 'Наименование',  dataIndex: 'name', width: 420, align: "left"
                        }
                    ]
                }

            ]
        }]
    }]
});

