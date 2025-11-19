<?php
// increment_story_view.php
include 'conn.php';
header("Content-Type: application/json");

$response = [];

if(isset($_POST['story_id'])){
    
    $storyId = mysqli_real_escape_string($conn, $_POST['story_id']);
    
    // Increment the views count
    $sql = "UPDATE stories SET views = views + 1 WHERE id = '$storyId'";
    
    if(mysqli_query($conn, $sql)){
        // Get updated view count
        $selectSql = "SELECT views FROM stories WHERE id = '$storyId'";
        $result = mysqli_query($conn, $selectSql);
        
        if($result && mysqli_num_rows($result) > 0){
            $row = mysqli_fetch_assoc($result);
            
            $response['statuscode'] = 200;
            $response['message'] = "View count updated";
            $response['views'] = $row['views'];
        } else {
            $response['statuscode'] = 404;
            $response['message'] = "Story not found";
        }
        
    } else {
        $response['statuscode'] = 500;
        $response['message'] = "Database error: " . mysqli_error($conn);
    }
    
} else {
    $response['statuscode'] = 400;
    $response['message'] = "Story ID missing";
}

echo json_encode($response);
?>