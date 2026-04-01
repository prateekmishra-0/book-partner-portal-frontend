$(document).ready(function() {
    // 1. Initialize DataTables
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

    // 2. Auto-open the Add Modal if there is a validation error from the backend
    if ($('#addStoreModal .is-invalid').length > 0) {
        var addModal = new bootstrap.Modal(document.getElementById('addStoreModal'));
        addModal.show();
    }

    // 3. Remove the red warning the second the user starts typing to fix their mistake
    $('#addStoreModal .is-invalid').on('input', function() {
        $(this).removeClass('is-invalid');
    });

    // 4. Wipe the form completely clean when the user clicks Cancel or closes the modal
    $('#addStoreModal').on('hidden.bs.modal', function () {
        $(this).find('form')[0].reset(); // Empties all the text boxes
        $(this).find('.is-invalid').removeClass('is-invalid'); // Removes the red outline and error text
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