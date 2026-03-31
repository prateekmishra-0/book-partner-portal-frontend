$(document).ready(function() {
    // Initialize DataTables
    $('#storeTable').DataTable({
        "pageLength": 10,
        "ordering": true,
        "info": true,
        "lengthChange": true,
        "searching": true,
        "columnDefs": [
            { "orderable": false, "targets": 5 }
        ],
        "language": {
            "search": "Search Anything:"
        }
    });
});

// Function to populate the Edit Modal with the correct row data
function populateEditModal(button) {
    document.getElementById('editStorId').value = button.getAttribute('data-id');
    document.getElementById('editStorName').value = button.getAttribute('data-name');
    document.getElementById('editStorAddress').value = button.getAttribute('data-address');
    document.getElementById('editCity').value = button.getAttribute('data-city');
    document.getElementById('editState').value = button.getAttribute('data-state');
    document.getElementById('editZip').value = button.getAttribute('data-zip');
}