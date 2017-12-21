/**
 * This class is the view model for the Main view of the application.
 */
var isLoadOrgStore=false;
var isLoadUkStore=false;
var isLoadPayordStore=false;
var isAddedPanels=false;

Ext.define('BillWebApp.view.main.MainModel', {
    extend: 'Ext.app.ViewModel',

    alias: 'viewmodel.main',

    data: {
        name: 'BillWebApp',
        loremIpsum: 'Редактирование платежек',
        periodId1: '',
        periodId2: '',
        orgId: '',
        tp: 'PAYORD_PERIODTP',

        // вариант списка типов для формы AskObjPanel
        formTp: 1
    },

    // формулы, для того, чтобы можно было менять data переменные
    formulas: {
        formTp: {
            set: function (value) {
                formTp=value;
            },
            get: function (get) {
                return this.formTp;
            },
        }
    },

    stores: {
        orgstore: {
            type: 'orgstore',
            listeners: {
                load: function() {
                    isLoadOrgStore=true;
                    console.log('OrgStore Loaded')
                    addPaneledit();
                }
            }
        },
        ukstore: {
            type: 'orgstore',
            proxy : {
                extraParams: {
                    tp: '1'
                }
            },
            listeners: {
                load: function() {
                    isLoadUkStore=true;
                    console.log('UkStore Loaded')
                    addPaneledit();
                }
            }
        },
        periodstore1: {
            type: 'periodstore1',
            proxy : {
                extraParams: {
                    repCd: 'RptPayDocList',
                    tp: '0'
                }
            }
        },
        periodstore2: {
            type: 'periodstore1',
            proxy : {
                extraParams: {
                    repCd: 'RptPayDocList',
                    tp: '0'
                }
            }
        },
        payordstore: {
            type: 'payordstore',
            proxy : {
                extraParams : {
                    payordGrpId : 0 // по умолчанию - все платежки
                }
            }
        },
        payordstore2: {
            type: 'payordstore',
            proxy : {
                extraParams : {
                    payordGrpId : 0 // все платежки
                }
            },
            listeners: {
                load: function() {
                    isLoadPayordStore=true;
                    console.log('PayordStore2 Loaded')
                }
            }
        },
        payordgrpstore: {
            type: 'payordgrpstore'
        },
        payordcmpstore: {
            type: 'payordcmpstore'
        },
        payordflowstore: {
            type: 'payordflowstore'
        },
        lststore: {
            type: 'lststore',
            proxy : {
                extraParams : {
                    tp : 'PAYORD_PERIODTP'
                }
            }
        },
        areastore: {
            type: 'lststore',
            proxy : {
                extraParams : {
                    tp : 'AREA_TP'
                }
            }
        },
        varstore: {
            type: 'lststore',
            proxy : {
                extraParams : {
                    tp : 'PAYORD_SRC_TP'
                }
            }
        },
        addrMainTpStore: { //список типов адресов ограниченный основными типами!
            type: 'addrtpstore',
            proxy : {
                extraParams : {
                    tp : '{formTp}' // байндить с data-переменной, которая может меняться
                }
            }
        },
        koAddrTpStore: {
            type: 'kostore'
        },
        servstore: {
            type: 'servstore'
        },
        orgcuruserstore: {
            type: 'orgcuruserstore'
        }
    }

});

function addPaneledit() {
    if (isAddedPanels!=true && isLoadOrgStore==true && isLoadUkStore==true
        && isLoadPayordStore==true) {
        isAddedPanels=true;
        var mainView = BillWebApp.getApplication().getMainView();
        mainView.add(
            [
                {
                    title: 'Редактирование',
                    iconCls: 'fa-edit',
                    reference: 'panelEdit',
                    xtype: 'panelEdit'
                }, {
                    title: 'Платежки',
                    iconCls: 'fa-inbox',
                    items: [{
                        xtype: 'panel1'
                    }]
                },
                {
                    title: 'Формирование',
                    iconCls: 'fa-cog',
                    items: [{
                        xtype: 'panel4'
                    }]
                },
                {
                    title: 'Настройки платежек',
                    iconCls: 'fa-cog',
                    xtype: 'panel5'
                },

                {
                    title: 'Параметры',
                    iconCls: 'fa-cog',
                    items: [{
                        xtype: 'panelPar'
                    }]
                }
            ]
        );
        mainView.doLayout;
    }
}