package client_controller;

import java.util.Random;
import javax.swing.JButton;

public class AI {
    private JButton[][] buttons;
    private int row = 20;
    private int col = 20;
    private Random rand = new Random();

    // Bảng điểm mới: Khoảng cách cực lớn để AI nhận diện mức độ nguy hiểm
    // 0, 1 quân, 2 quân, 3 quân, 4 quân, 5 quân (Thắng)
    private final long[] attackScore = {0, 10, 100, 1000, 10000, 1000000};
    private final long[] defenseScore = {0, 8, 80, 800, 8000, 800000};

    public AI(JButton[][] buttons) {
        this.buttons = buttons;
    }

    public int[] findBestMove(int difficulty) {
        if (difficulty == 1) return findRandomMove();

        int[] bestMove = new int[]{-1, -1};
        long maxScore = -1;

        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                if (buttons[i][j].getText().equals("")) {
                    long attScore = calculateScore(i, j, "O", attackScore); // Máy là O
                    long defScore = calculateScore(i, j, "X", defenseScore); // Người là X
                    long tempScore;

                    if (difficulty == 2) {
                        // Bình thường: Thủ và công ngang nhau
                        tempScore = attScore + defScore;
                    } else {
                        // KHÓ: Nếu địch có nước nguy hiểm (4 con hoặc 3 con thoáng), ưu tiên thủ tuyệt đối
                        // Nếu mình có nước thắng (5 con), ưu tiên công tuyệt đối
                        if (attScore >= 1000000) tempScore = attScore * 2; // Ưu tiên thắng ngay
                        else if (defScore >= 8000) tempScore = defScore * 2; // Ép máy phải chặn nước 4 của người
                        else tempScore = attScore + defScore;
                    }

                    if (tempScore > maxScore) {
                        maxScore = tempScore;
                        bestMove[0] = i;
                        bestMove[1] = j;
                    }
                }
            }
        }
        
        if (bestMove[0] == -1) return findRandomMove();
        return bestMove;
    }

    private int[] findRandomMove() {
        int x, y;
        int count = 0;
        do {
            x = rand.nextInt(row);
            y = rand.nextInt(col);
            count++;
            if(count > 1000) break; // Tránh vòng lặp vô tận nếu bàn cờ gần đầy
        } while (!buttons[x][y].getText().equals(""));
        return new int[]{x, y};
    }

    private long calculateScore(int x, int y, String side, long[] scoreTable) {
        long totalScore = 0;
        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
        
        for (int[] dir : directions) {
            int count = 0; // Số quân ta đang có trên đường này
            int blocked = 0; // Số đầu bị chặn
            
            // Duyệt chiều dương
            for (int k = 1; k < 5; k++) {
                int nx = x + k * dir[0];
                int ny = y + k * dir[1];
                if (!isValid(nx, ny)) { blocked++; break; }
                if (buttons[nx][ny].getText().equals(side)) count++;
                else if (!buttons[nx][ny].getText().equals("")) { blocked++; break; }
                else break; 
            }
            
            // Duyệt chiều âm
            for (int k = 1; k < 5; k++) {
                int nx = x - k * dir[0];
                int ny = y - k * dir[1];
                if (!isValid(nx, ny)) { blocked++; break; }
                if (buttons[nx][ny].getText().equals(side)) count++;
                else if (!buttons[nx][ny].getText().equals("")) { blocked++; break; }
                else break;
            }
            
            // Nếu đã đạt 4 quân (đánh thêm ô này là 5) thì thắng luôn, không xét blocked
            if (count >= 4) {
                totalScore += scoreTable[5]; 
            } else if (blocked < 2) {
                // Nếu chưa đủ 5 quân, thì chỉ những đường chưa bị chặn 2 đầu mới có giá trị
                long currentScore = scoreTable[count];
                
                // Thưởng điểm cho thế cờ thoáng (không bị chặn đầu nào)
                if (blocked == 0) {
                    if (count == 2) currentScore *= 5; // Thế 3 thoáng (2+1)
                    if (count == 3) currentScore *= 10; // Thế 4 thoáng (3+1)
                }
                
                totalScore += currentScore;
            }
        }
        return totalScore;
    }

    private boolean isValid(int x, int y) {
        return x >= 0 && x < row && y >= 0 && y < col;
    }
}