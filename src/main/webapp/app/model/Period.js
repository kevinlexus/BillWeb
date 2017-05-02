/**
 * Created by lev on 13.02.2017.
 */
Ext.define('BillWebApp.model.Period', {
    extend: 'Ext.data.Model',
    fields: [
        { name: 'id', type: 'int', convert: null },
        { name: 'period', type: 'string' }
    ]
});