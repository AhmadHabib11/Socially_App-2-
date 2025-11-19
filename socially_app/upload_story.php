<?php
// upload_story.php
include 'conn.php';
header("Content-Type: application/json");

// Set timezone to match your local timezone
date_default_timezone_set('Asia/Karachi'); // Pakistan timezone

$response = [];

if(isset($_POST['user_id']) && isset($_POST['media_data']) && !empty($_POST['media_data'])){
    
    $userId = mysqli_real_escape_string($conn, $_POST['user_id']);
    $mediaData = $_POST['media_data'];
    $mediaType = isset($_POST['media_type']) ? $_POST['media_type'] : 'image';
    
    // Decode base64 image/video data
    $decodedData = base64_decode($mediaData);
    
    if($decodedData === false){
        $response['statuscode'] = 400;
        $response['message'] = "Invalid media data";
        echo json_encode($response);
        exit;
    }
    
    // Create uploads directory for stories if it doesn't exist
    $uploadDir = "uploads/stories/";
    if(!file_exists($uploadDir)){
        mkdir($uploadDir, 0777, true);
    }
    
    // Generate unique filename
    $extension = ($mediaType === 'video') ? '.mp4' : '.jpg';
    $fileName = uniqid() . '_' . time() . $extension;
    $filePath = $uploadDir . $fileName;
    
    // Save media file
    if(file_put_contents($filePath, $decodedData)){
        
        // Calculate expiry time (exactly 24 hours = 86400 seconds from now)
        $currentTimestamp = time();
        $expiryTimestamp = $currentTimestamp + 86400; // Add exactly 24 hours (86400 seconds)
        $expiresAt = date('Y-m-d H:i:s', $expiryTimestamp);
        $createdAt = date('Y-m-d H:i:s', $currentTimestamp);
        
        // Insert story record into database with explicit created_at
        $sql = "INSERT INTO stories (user_id, media_path, media_type, created_at, expires_at) 
                VALUES ('$userId', '$filePath', '$mediaType', '$createdAt', '$expiresAt')";
        
        if(mysqli_query($conn, $sql)){
            $storyId = mysqli_insert_id($conn);
            
            $response['statuscode'] = 200;
            $response['message'] = "Story uploaded successfully";
            $response['story'] = [
                'id' => $storyId,
                'media_path' => $filePath,
                'created_at' => $createdAt,
                'expires_at' => $expiresAt,
                'hours_until_expiry' => 24
            ];
        } else {
            // If database insert fails, delete the uploaded file
            unlink($filePath);
            
            $response['statuscode'] = 500;
            $response['message'] = "Database error: " . mysqli_error($conn);
        }
        
    } else {
        $response['statuscode'] = 500;
        $response['message'] = "Failed to save media file";
    }
    
} else {
    $response['statuscode'] = 400;
    $response['message'] = "Required fields missing (user_id, media_data)";
}

echo json_encode($response);
?>