
<?php
// conn.php
$servername = "localhost";
$username = "root";
$password = "";
$dbname = "socially_db";

$conn = mysqli_connect($servername, $username, $password, $dbname);

if (!$conn) {
    die(json_encode(["error" => "Connection failed: " . mysqli_connect_error()]));
}
?>


<?php