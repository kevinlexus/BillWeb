Ext.define('BillWebApp.view.main.Panel2', {
    extend: 'Ext.panel.Panel',
    xtype: 'panel2',
    title: 'Редактирование поручений',
    layout: {
        type: 'vbox' // после смены vbox, проверить, как открываются комбо-боксы (могут не работать)
    },
    width: 1010,
    minHeight: 500,
    bodyPadding: 10,
    reference: 'panel2',
    controller: 'panel2controller',
    defaults: {
        frame: true,
        bodyPadding: 10,
        listeners:{
            //beforerender: 'onPanel2BeforeRenderer'
        }
    },
    items: [{
            // Платежные поручения в банк
            xtype: 'gridpanel',
            reference: 'payordFlowGrid',
            width: 1000,
            minHeight: 120,
            margin: '0 0 10 0',
            tbar: [{
                text: 'Добавить поручение',
                handler: 'onGridPayordFlowAdd'
            }, {
                text: 'Сохранить',
                handler: 'onGridPayordFlowUpd'
            }, {
                text: 'Отменить',
                handler: 'onGridPayordFlowCancel'
            },{
                xtype: 'datefield',
                name: 'genDt',
                reference: 'genDt2',
                fieldLabel: 'Дата',
                allowBlank: true,
                format: 'd.m.Y'
            },
            {
                text: 'Обновить',
                handler: 'onGridPayordFlowRefresh'
            }],
            requires: [
                'Ext.selection.CellModel',
                'Ext.grid.column.Action'
            ],
            plugins: {
                ptype: 'cellediting',
                clicksToEdit: 1,
                listeners: {
                    beforeedit: function(editor, context, eOpts){
                        // workaround for error at clicking a widgetcolumn
                        if (context.column.widget)
                            return false;
                    }
                }
            },
            bind: {
                store: '{payordflowstore}',
                listeners: {
                   // edit: 'onGridPayordGrpUpd',
                   // cancelEdit: 'onGridPayordGrpCancel'
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
        },
        {
            // Управление
            xtype: 'panel',
            width: 1000,
            minHeight: 20,
            tbar: [{
                text: 'Подписать все',
                handler: 'onGridPayordFlowSignAll'
            }, {
                text: 'Снять подпись',
                handler: 'onGridPayordFlowUnSignAll'
            }]
        }
    ]

});

