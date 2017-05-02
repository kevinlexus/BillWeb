Ext.define('BillWebApp.view.main.Panel3', {
    extend: 'Ext.panel.Panel',
    xtype: 'panel3',
    title: 'Настройки платежек',
    layout: {
        type: 'vbox',
        pack: 'start',
        align: 'stretch'
    },
    width: 1010,
    minHeight: 900,
    bodyPadding: 10,
    reference: 'panel3',
    controller: 'panel3controller',
    defaults: {
        frame: true,
        bodyPadding: 10
    },

    items: [{
            // ГРУППЫ ПЛАТЕЖЕК
            xtype: 'gridpanel',
            reference: 'payordGrpGrid',
            width: 1000,
            margin: '0 0 10 0',
            tbar: [{
                text: 'Добавить группу',
                handler: 'onGridPayordGrpAdd'
            }, {
                text: 'Сохранить',
                handler: 'onGridPayordGrpUpd'
            }, {
                text: 'Отменить',
                handler: 'onGridPayordGrpCancel'
            }],
            requires: [
                'Ext.selection.CellModel',
                'Ext.grid.column.Action'
            ],
            plugins: {
                ptype: 'cellediting',
                clicksToEdit: 1
            },
            //selModel: 'cellmodel',
            /*plugins: {
                ptype: 'rowediting',
                clicksToEdit: 2,
                saveBtnText: 'Сохранить',
                cancelBtnText: 'Отмена',
                errorSummary: false // погасить сообщение валидации
            },*/
            bind: {
                store: '{payordgrpstore}',
                listeners: {
                   // edit: 'onGridPayordGrpUpd',
                   // cancelEdit: 'onGridPayordGrpCancel'
                }
            },
            actions: {
                del: {
                    glyph: 'xf147@FontAwesome',
                    tooltip: 'Удалить',
                    handler: 'onGridPayordGrpDel'
                }
            },
            listeners: {
                // клик по строчке группы платежки, отобразить в дочернем гриде формулы
                rowclick: 'onGridPayordGrpRowClick'
            },
            columns: [
                { text: 'Id',  dataIndex: 'id', width: 50,
                    editor: {
                        allowBlank: true
                    }
                },
                { text: 'Наименование',  dataIndex: 'name', width: 400, align: "left",
                    editor: {
                        allowBlank: false
                    }
                },
                { text: 'Создано',  dataIndex: 'dtf', width: 100, align: "left",
                    formatter: 'date("d-m-Y H:i:s")'
                },
                { text: 'Пользователь',  dataIndex: 'username', width: 100, align: "left"
                },
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
        // ПЛАТЕЖКИ
        xtype: 'gridpanel',
        iconCls: 'framing-buttons-grid',
        reference: 'payordGrid',
        width: 1000,
        //maxHeight:400,
        header: false,
        margin: '0 0 10 0',
        tbar: [{
            text: 'Добавить платежку',
            handler: 'onGridPayordAdd'
        }, {
            text: 'Сохранить',
            handler: 'onGridPayordUpd'
        }, {
            text: 'Отменить',
            handler: 'onGridPayordCancel'
        }],
        requires: [
            'Ext.selection.CellModel',
            'Ext.grid.column.Action'
        ],
        plugins: {
            ptype: 'cellediting',
            clicksToEdit: 1
        },
        bind: {
            store: '{payordstore}',
            listeners: {
            }
        },
        actions: {
            del: {
                glyph: 'xf147@FontAwesome',
                tooltip: 'Удалить',
                handler: 'onGridPayordDel'
            }
        },
        listeners: {
            // клик по строчке платежки, отобразить в дочернем гриде формулы
            rowclick: 'onGridPayordRowClick'
        },
        columns: [
            { text: 'Id',  dataIndex: 'id', width: 50
            },
            { text: 'Наименование',  dataIndex: 'name', width: 400, align: "left",
                editor: {
                    allowBlank: false
                }
            },
            {
                text: 'Периодичность',
                dataIndex: 'periodTpFk',
                width: 170, align: "left",
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
                        store: '{lststore}'
                    },
                    allowBlank: false
                },
                  renderer: 'onGridPayordPeriodRender'
            },

            { text: 'Дни формир.',  dataIndex: 'selDays', width: 100, align: "left",
                editor: {
                    allowBlank: true
                }
            },
            { text: 'Формула',  dataIndex: 'formula', width: 100, align: "left",
                editor: {
                    allowBlank: true
                }
            }
            ,
            { text: 'Сумма',  dataIndex: 'summa', width: 70, align: "left"
            },
            { text: 'Создано',  dataIndex: 'dtf', width: 100, align: "left",
                formatter: 'date("d-m-Y H:i:s")'
            },
            { text: 'Пользователь',  dataIndex: 'username', width: 100, align: "left"
            },
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
            // ФОРМУЛЫ ПЛАТЕЖЕК
            xtype: 'gridpanel',
            iconCls: 'framing-buttons-grid',
            reference: 'payordCmpGrid',

            width: 1000,
            header: false,
            margin: '0 0 10 0',
            tbar: [{
                text: 'Добавить формулу',
                handler: 'onGridPayordCmpAdd'
            }, {
                text: 'Сохранить',
                handler: 'onGridPayordCmpUpd'
            }, {
                text: 'Отменить',
                handler: 'onGridPayordCmpCancel'
            }],
            requires: [
                'Ext.selection.CellModel',
                'Ext.grid.column.Action'
            ],
            //selModel: 'cellmodel',
            plugins: {
                ptype: 'cellediting',
                clicksToEdit: 1
            },
            bind: {
                store: '{payordcmpstore}'
            },
            actions: {
                del: {
                    glyph: 'xf147@FontAwesome',
                    tooltip: 'Удалить',
                    handler: 'onGridPayordCmpDel'
                }
            },
            listeners: {
                //validateedit: function(editor, e, eOpts) { // здесь писать проверку ввода данных!
                //    check1(editor, e, eOpts);
                //    console.log('editor:'+editor.name+' field: '+e.field +' = ' + e.value);
                //}
            },

            columns: [
                { text: 'Id',  dataIndex: 'id', width: 50
                },
                {
                    header: 'Вар.сбора',
                    dataIndex: 'varFk',
                    width: 200,
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
                        listConfig:{
                            minWidth:500
                        },
                        bind: {
                            store: '{varstore}'
                        },
                        allowBlank: false
                    },
                    renderer: 'onGridPayordCmpVarRender'
                },
                {
                    header: 'Услуга',
                    dataIndex: 'servFk',
                    width: 200,
                    queryMode: 'local',
                    editor: {
                        xtype: 'combo',
                        typeAhead: true,
                        forceSelection: true,
                        displayField: 'name',
                        valueField: 'id',
                        triggerAction: 'all',
                        listConfig:{
                            minWidth:500
                        },
                        bind: {
                            store: '{servstore}'
                        },
                        allowBlank: true
                    },
                    renderer: 'onGridPayordCmpServRender'
                },
                {
                    header: 'Организация',
                    dataIndex: 'orgFk',
                    width: 200,
                    queryMode: 'local',
                    editor: {
                        xtype: 'combo',
                        typeAhead: true,
                        forceSelection: true,
                        displayField: 'name',
                        valueField: 'id',
                        triggerAction: 'all',
                        allowBlank: false,
                        listConfig:{
                            minWidth:500
                        },
                        bind: {
                            store: '{orgstore}'
                        }
                    },
                    renderer: 'onGridPayordCmpOrgRender'
                },
                {
                    header: 'koFk',
                    dataIndex: 'koFk',
                    width: 0// 0 - чтобы не было видно поле, а поле нужно, иначе не отработает сохранение в store (жесть)
                },
                {
                    header: 'Объект',
                    dataIndex: 'koName',
                    width: 200,
                    queryMode: 'local',
                    editor: {
                        xtype: 'combo',
                        typeAhead: true,
                        forceSelection: true,
                        triggerAction: 'all',
                        allowBlank: true,
                        listeners: {
                            expand: 'onGridPayordCmpItemSel'
                        }
                    }
                },
                {
                    header: 'Маркер',  dataIndex: 'mark', width: 50, align: "left",
                    editor: {
                        allowBlank: false
                    }
                },
                { text: 'Сумма',  dataIndex: 'summa', width: 70, align: "left"
                },
                { text: 'Создано',  dataIndex: 'dtf', width: 100, align: "left",
                    formatter: 'date("d-m-Y H:i:s")'
                },
                { text: 'Пользователь',  dataIndex: 'username', width: 100, align: "left"
                },
                {
                    menuDisabled: true,
                    sortable: false,
                    xtype: 'actioncolumn',
                    width: 50,
                    items: ['@del']
                }
            ]
        }

    ]

});
