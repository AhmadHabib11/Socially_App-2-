<?php
// signup.php
include 'conn.php';
header("Content-Type: application/json");

$response = [];

if(
    isset($_POST['username'], $_POST['first_name'], $_POST['last_name'], 
    $_POST['dob'], $_POST['email'], $_POST['password'])
){
    $username   = mysqli_real_escape_string($conn, $_POST['username']);
    $fname      = mysqli_real_escape_string($conn, $_POST['first_name']);
    $lname      = mysqli_real_escape_string($conn, $_POST['last_name']);
    $dob        = mysqli_real_escape_string($conn, $_POST['dob']);
    $email      = mysqli_real_escape_string($conn, $_POST['email']);
    $passwordHash = password_hash($_POST['password'], PASSWORD_BCRYPT);
    
    // Handle profile picture - save as file
    $profilePicPath = "";
    
    if(isset($_POST['profile_pic']) && !empty($_POST['profile_pic'])){
        $imageData = $_POST['profile_pic'];
        
        // Decode base64
        $imageData = base64_decode($imageData);
        
        if($imageData !== false){
            // Create uploads directory if it doesn't exist
            $uploadDir = "uploads/profiles/";
            if(!file_exists($uploadDir)){
                mkdir($uploadDir, 0777, true);
            }
            
            // Generate unique filename
            $fileName = uniqid() . '_' . time() . '.png';
            $filePath = $uploadDir . $fileName;
            
            // Save image to file
            if(file_put_contents($filePath, $imageData)){
                $profilePicPath = $filePath;
            } else {
                $response['statuscode'] = 500;
                $response['message'] = "Failed to save profile picture";
                echo json_encode($response);
                exit;
            }
        }
    }

    // Check if email or username exists
    $check = mysqli_query($conn, 
        "SELECT id FROM users WHERE email='$email' OR username='$username'"
    );

    if(mysqli_num_rows($check) > 0){
        $response['statuscode'] = 409;
        $response['message'] = "Email or username already exists";
        echo json_encode($response);
        exit;
    }

    // Insert user with profile picture path
    $sql = "INSERT INTO users(username, first_name, last_name, dob, email, password, profile_pic)
            VALUES('$username', '$fname', '$lname', '$dob', '$email', '$passwordHash', '$profilePicPath')";

    if(mysqli_query($conn, $sql)){
        $response['statuscode'] = 200;
        $response['message'] = "Signup successful";
        $response['userid'] = mysqli_insert_id($conn);
    } else {
        $response['statuscode'] = 500;
        $response['message'] = "Database error: " . mysqli_error($conn);
    }

} else {
    $response['statuscode'] = 400;
    $response['message'] = "Required fields missing";
}

echo json_encode($response);
?>