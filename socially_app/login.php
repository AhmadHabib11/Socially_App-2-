
<?php
// login.php
include 'conn.php';
header("Content-Type: application/json");

$response = [];

if(isset($_POST['identifier'], $_POST['password'])){
    $identifier = mysqli_real_escape_string($conn, $_POST['identifier']);
    $password   = $_POST['password'];

    $sql = "SELECT id, username, first_name, last_name, email, password, profile_pic 
            FROM users 
            WHERE email='$identifier' OR username='$identifier'";

    $result = mysqli_query($conn, $sql);

    if(mysqli_num_rows($result) == 1){
        $row = mysqli_fetch_assoc($result);

        if(password_verify($password, $row['password'])){
            $response['statuscode'] = 200;
            $response['message'] = "Login successful";
            
            $response['user'] = [
                "id" => $row['id'],
                "username" => $row['username'],
                "first_name" => $row['first_name'],
                "last_name" => $row['last_name'],
                "email" => $row['email'],
                "profile_pic" => $row['profile_pic']
            ];
        } else {
            $response['statuscode'] = 401;
            $response['message'] = "Incorrect password";
        }

    } else {
        $response['statuscode'] = 404;
        $response['message'] = "Account not found";
    }

} else {
    $response['statuscode'] = 400;
    $response['message'] = "Required fields missing";
}

echo json_encode($response);
?>