Ext.define('BillWebApp.view.main.Panel2', {
    extend: 'Ext.panel.Panel',
    xtype: 'panel2',
    title: 'Редактирование поручений',
    layout: {
        type: 'vbox',
        pack: 'start',
        align: 'stretch'
    },
/*    listeners: {
        beforerender : 'onPanel2BeforeRender'
    },*/
    width: 1010,
    minHeight: 2000,
    bodyPadding: 10,
    reference: 'panel2',
    controller: 'panel2controller',
    defaults: {
        frame: true,
        bodyPadding: 10
    },
    items: [{
            // Платежные поручения в банк
            xtype: 'gridpanel',
            reference: 'payordFlowGrid',
            width: 1000,
            //minHeight: 120,
            margin: '0 0 10 0',
            bind: {
                store: '{payordflowstore}'
            },
            tbar: [{
                text: 'Добавить',
                handler: 'onGridPayordFlowAdd'
            }, {
                text: 'Сохранить',
                handler: 'onGridPayordFlowUpd'
            }, {
                text: 'Отменить',
                handler: 'onGridPayordFlowCancel'
            }, {
                xtype: 'combobox',
                fieldLabel: 'Ук',
                displayField: 'name',
                valueField: 'id',
                reference: 'fltUk',
                matchFieldWidth: false,
                labelWidth: 15,
                width: 185,

                queryMode: 'local',
                selectOnFocus: true,
                listeners: {
                    beforequery: function (record) {
                        record.query = new RegExp(record.query, 'i');
                        record.forceAll = true;
                    }
                },
                bind: {
                    store: '{ukstore}',
                    value: '{id}'
                },
            },
                {
                xtype: 'datefield',
                name: 'genDt2',
                reference: 'genDt2',
                startDay : 1,
                fieldLabel: 'Период с',
                labelWidth: 60,
                width: 185,
                allowBlank: true,
                format: 'd.m.Y',
                value: new Date()
            },{
                xtype: 'datefield',
                name: 'genDt3',
                reference: 'genDt3',
                startDay : 1,
                fieldLabel: 'по',
                labelWidth: 15,
                width: 140,
                allowBlank: true,
                format: 'd.m.Y',
                value: new Date()
            },
            {
                text: 'Обновить',
                handler: 'onGridPayordFlowRefresh'
            },{
                text: 'Печать',
                handler: 'onGridPayordPrint'
            }],
            requires: [
                'Ext.selection.CellModel',
                'Ext.grid.column.Action'
            ],
            plugins: {
                ptype: 'cellediting',
                clicksToEdit: 1,
                listeners: {
                    beforeedit: 'onPayordFlowGridBeforeEdit'
                    /*beforeedit: function(editor, context, eOpts){
                        // workaround for error at clicking a widgetcolumn
                        if (context.column.widget)
                            return false;
                    }*/
                }
            },
            actions: {
                del: {
                    glyph: 'xf147@FontAwesome',
                    tooltip: 'Удалить',
                    handler: 'onGridPayordFlowDel'
                }
            },
            columns: [
                { text: 'Id',  dataIndex: 'id', width: 50
                },
                {
                    text: 'Платежка',
                    dataIndex: 'payordFk',
                    width: 250, align: "left",
                    queryMode: 'local',
                    editor: {
                        xtype: 'combo',
                        typeAhead: true,
                        forceSelection: true,
                        displayField: 'name',
                        valueField: 'id',
                        triggerAction: 'all',
                        validator: function(value) {
                            if (value != '') {
                                return true;
                            } else {
                                return 'Необходимо заполнить поле!';
                            }
                        },
                        bind: {
                            store: '{payordstore}'
                        },
                        listConfig:{
                            minWidth:500
                        },
                        allowBlank: false
                    },
                    renderer: 'onGridPayordFlowPayordRender'
                },
                {
                    text: 'УК',
                    dataIndex: 'ukFk',
                    width: 150, align: "left",
                    queryMode: 'local',
                    editor: {
                        xtype: 'combo',
                        typeAhead: true,
                        forceSelection: true,
                        displayField: 'name',
                        valueField: 'id',
                        triggerAction: 'all',
                        validator: function(value) {
                            if (value != '') {
                                return true;
                            } else {
                                return 'Необходимо заполнить поле!';
                            }
                        },
                        bind: {
                            store: '{orgstore}'
                        },
                        listConfig:{
                            minWidth:500
                        },
                        allowBlank: false
                    },
                    renderer: 'onGridPayordFlowUkRender'
                },
                { text: 'Сумма к перечисл.',  dataIndex: 'summa', width: 150,
                    editor: {
                        allowBlank: true
                    }
                },
                { text: '№ п.п.',  dataIndex: 'npp', width: 100,
                    editor: {
                        allowBlank: true
                    }
                },
                { xtype: 'datecolumn',
                    header: 'Дата',
                    dataIndex: 'dt',

                    width: 95,
                    format: 'd m Y',
                    editor: {
                        xtype: 'datefield',
                        format: 'd.m.y'
                    }
                },
                { text: 'Расчет.сумма',  dataIndex: 'summa6', width: 100
                },
                { xtype: 'checkcolumn', text: 'Подпись', dataIndex: 'signed' }
                ,
                 {
                    menuDisabled: true,
                    sortable: false,
                    xtype: 'actioncolumn',
                    width: 50,
                    items: ['@del']
                }
            ]
        }/*,
        {
            // Управление
            xtype: 'panel',
            //width: 1000,
            //minHeight: 20,
            tbar: [{
                text: 'Печать',
                handler: 'onGridPayordPrint'
            }{
                text: 'Подписать все',
                handler: 'onGridPayordFlowSignAll'
            }, {
                text: 'Снять подпись',
                handler: 'onGridPayordFlowUnSignAll'
            }]
        }*/
    ]

});

