<?php
// get_stories.php
include 'conn.php';
header("Content-Type: application/json");

$response = [];

// Optional: Get stories for a specific user, or all stories
$userId = isset($_GET['user_id']) ? mysqli_real_escape_string($conn, $_GET['user_id']) : null;

// Build query to get active stories (not expired, is_active = 1)
$currentTime = date('Y-m-d H:i:s');

if($userId){
    // Get stories for a specific user
    $sql = "SELECT s.*, u.username, u.first_name, u.last_name, u.profile_pic 
            FROM stories s
            INNER JOIN users u ON s.user_id = u.id
            WHERE s.user_id = '$userId' 
            AND s.expires_at > '$currentTime' 
            AND s.is_active = 1
            ORDER BY s.created_at DESC";
} else {
    // Get all active stories, grouped by user
    $sql = "SELECT s.*, u.username, u.first_name, u.last_name, u.profile_pic 
            FROM stories s
            INNER JOIN users u ON s.user_id = u.id
            WHERE s.expires_at > '$currentTime' 
            AND s.is_active = 1
            ORDER BY s.user_id, s.created_at DESC";
}

$result = mysqli_query($conn, $sql);

if($result){
    $stories = [];
    
    while($row = mysqli_fetch_assoc($result)){
        // Read the image file and convert to base64
        $mediaBase64 = "";
        if(file_exists($row['media_path'])){
            $mediaData = file_get_contents($row['media_path']);
            $mediaBase64 = base64_encode($mediaData);
        }
        
        $stories[] = [
            'id' => $row['id'],
            'user_id' => $row['user_id'],
            'username' => $row['username'],
            'first_name' => $row['first_name'],
            'last_name' => $row['last_name'],
            'profile_pic' => $row['profile_pic'],
            'media_path' => $row['media_path'],
            'media_type' => $row['media_type'],
            'media_base64' => $mediaBase64,
            'created_at' => $row['created_at'],
            'expires_at' => $row['expires_at'],
            'views' => $row['views']
        ];
    }
    
    $response['statuscode'] = 200;
    $response['message'] = "Stories fetched successfully";
    $response['stories'] = $stories;
    $response['count'] = count($stories);
    
} else {
    $response['statuscode'] = 500;
    $response['message'] = "Database error: " . mysqli_error($conn);
}

echo json_encode($response);
?>