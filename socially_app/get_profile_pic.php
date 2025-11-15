<?php
// get_profile_pic.php
header("Content-Type: application/json");

$response = [];

if(isset($_GET['path']) && !empty($_GET['path'])){
    $filePath = $_GET['path'];
    
    // Security: Make sure the path is within uploads directory
    if(strpos($filePath, 'uploads/profiles/') !== 0){
        $response['statuscode'] = 403;
        $response['message'] = "Invalid path";
        echo json_encode($response);
        exit;
    }
    
    if(file_exists($filePath)){
        $imageData = file_get_contents($filePath);
        $base64Image = base64_encode($imageData);
        
        $response['statuscode'] = 200;
        $response['image'] = $base64Image;
    } else {
        $response['statuscode'] = 404;
        $response['message'] = "Image not found";
    }
} else {
    $response['statuscode'] = 400;
    $response['message'] = "Path parameter missing";
}

echo json_encode($response);
?>