var updateStats = function (serviceURL, id) {
    invokerUtil.get(
        serviceURL,
        function (data) {
            $(id).html(data);
        }, function (message) {
            console.log(message);
        }
    );
};
//TODO : Refactor the users/count API to remove tenant-domain parameter
$(document).ready(function(){
    updateStats("/mdm-admin/devices/count", "#device-count");
    updateStats("/mdm-admin/policies/count", "#policy-count");
    updateStats("/mdm-admin/users/count/" + "carbon.super", "#user-count");
});